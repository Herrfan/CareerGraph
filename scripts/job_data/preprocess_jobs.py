from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path
from typing import Any


CITY_ALIASES = {
    "杭州": "Hangzhou",
    "杭州市": "Hangzhou",
    "上海": "Shanghai",
    "上海市": "Shanghai",
    "北京": "Beijing",
    "北京市": "Beijing",
    "深圳": "Shenzhen",
    "深圳市": "Shenzhen",
    "广州": "Guangzhou",
    "广州市": "Guangzhou",
}

EDUCATION_ALIASES = {
    "大专": "college",
    "专科": "college",
    "本科": "bachelor",
    "学士": "bachelor",
    "硕士": "master",
    "研究生": "master",
    "博士": "doctor",
}

SKILL_KEYWORDS = [
    "Java",
    "Spring",
    "Spring Boot",
    "MySQL",
    "Redis",
    "Kafka",
    "Python",
    "Django",
    "Flask",
    "JavaScript",
    "TypeScript",
    "Vue",
    "React",
    "Node.js",
    "Linux",
    "Docker",
    "Kubernetes",
    "SQL",
    "Postman",
    "Selenium",
    "JMeter",
    "Excel",
]

NOISE_PATTERNS = [
    re.compile(r"课程|培训|招生|咨询顾问"),
    re.compile(r"招聘会|宣讲会|双选会"),
    re.compile(r"兼职日结|刷单|无需经验高薪"),
]


@dataclass
class NormalizedJobRecord:
    source_id: str
    source_platform: str
    source_url: str
    row_hash: str
    company_name: str
    job_title: str
    job_category: str
    industry: str
    city: str
    salary_text: str
    salary_min: int | None
    salary_max: int | None
    education_level: str
    experience_text: str
    job_description: str
    skills: list[str]
    published_at: str
    crawled_at: str
    is_valid: bool
    invalid_reason: str
    confidence_score: float


def parse_args() -> argparse.Namespace:
    base_dir = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(description="Preprocess job data into normalized JSONL.")
    parser.add_argument("--input", required=True, help="Path to source xls/xlsx/csv/json file")
    parser.add_argument("--output-dir", required=True, help="Directory for normalized outputs")
    parser.add_argument("--source-platform", default="excel_import", help="Source platform label")
    parser.add_argument("--sheet-name", default=None, help="Optional sheet name for Excel")
    parser.add_argument("--sample-limit", type=int, default=None, help="Optional row limit for debugging")
    parser.add_argument("--ai-review", action="store_true", help="Enable optional AI review hook for low-confidence rows")
    parser.add_argument("--mapping", default=str(base_dir / "field_mapping.example.json"), help="Optional field mapping file")
    parser.add_argument("--city-aliases", default=str(base_dir / "config" / "city_aliases.json"), help="Optional city alias config")
    parser.add_argument("--education-aliases", default=str(base_dir / "config" / "education_aliases.json"), help="Optional education alias config")
    parser.add_argument("--skill-keywords", default=str(base_dir / "config" / "skill_keywords.txt"), help="Optional skill keyword file")
    return parser.parse_args()


def load_json_map(path: str, fallback: dict[str, str]) -> dict[str, str]:
    file_path = Path(path)
    if not file_path.exists():
        return fallback
    with file_path.open("r", encoding="utf-8") as f:
        data = json.load(f)
    return {str(k): str(v) for k, v in data.items()}


def load_skill_keywords(path: str, fallback: list[str]) -> list[str]:
    file_path = Path(path)
    if not file_path.exists():
        return fallback
    values = []
    with file_path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#"):
                values.append(line)
    return values or fallback


def load_field_mapping(path: str) -> dict[str, list[str]]:
    file_path = Path(path)
    if not file_path.exists():
        return {}
    with file_path.open("r", encoding="utf-8") as f:
        data = json.load(f)
    result = {}
    for key, value in data.items():
        if isinstance(value, list):
            result[key] = [str(item) for item in value]
        elif isinstance(value, str):
            result[key] = [value]
    return result


def read_rows(input_path: Path, sheet_name: str | None) -> list[dict[str, Any]]:
    suffix = input_path.suffix.lower()
    if suffix in {".xls", ".xlsx"}:
        try:
            import pandas as pd  # type: ignore
        except ImportError as exc:
            raise RuntimeError("Excel preprocessing requires pandas and xlrd/openpyxl in your Python env.") from exc
        df = pd.read_excel(input_path, sheet_name=sheet_name or 0)
        return df.fillna("").to_dict(orient="records")

    if suffix == ".csv":
        try:
            import pandas as pd  # type: ignore
        except ImportError as exc:
            raise RuntimeError("CSV preprocessing requires pandas in your Python env.") from exc
        df = pd.read_csv(input_path)
        return df.fillna("").to_dict(orient="records")

    if suffix in {".json", ".jsonl"}:
        if suffix == ".jsonl":
            rows = []
            with input_path.open("r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line:
                        rows.append(json.loads(line))
            return rows
        with input_path.open("r", encoding="utf-8") as f:
            data = json.load(f)
        if isinstance(data, list):
            return data
        raise RuntimeError("JSON input must be an array of rows.")

    raise RuntimeError(f"Unsupported input format: {input_path.suffix}")


def clean_text(value: Any) -> str:
    text = "" if value is None else str(value)
    text = re.sub(r"<[^>]+>", " ", text)
    text = text.replace("\u3000", " ")
    text = re.sub(r"[\x00-\x08\x0b\x0c\x0e-\x1f]", " ", text)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def normalize_city(city: str, city_aliases: dict[str, str]) -> str:
    city = clean_text(city)
    return city_aliases.get(city, city)


def normalize_education(text: str, education_aliases: dict[str, str]) -> str:
    text = clean_text(text)
    for key, value in education_aliases.items():
        if key in text:
            return value
    return text.lower()


def parse_salary(text: str) -> tuple[int | None, int | None]:
    raw = clean_text(text).lower()
    if not raw:
        return None, None
    normalized = raw.replace("k", "000").replace("千", "000").replace("万", "0000")
    numbers = [int(match) for match in re.findall(r"\d+", normalized)]
    if not numbers:
        return None, None
    if len(numbers) == 1:
        return numbers[0], numbers[0]
    left, right = numbers[0], numbers[1]
    if left > right:
        left, right = right, left
    return left, right


def infer_category(job_title: str, description: str, skills: list[str]) -> str:
    haystack = " ".join([job_title, description, " ".join(skills)]).lower()
    if "java" in haystack or "spring" in haystack:
        return "backend"
    if "vue" in haystack or "react" in haystack or "frontend" in haystack or "前端" in haystack:
        return "frontend"
    if "test" in haystack or "测试" in haystack or "selenium" in haystack:
        return "testing"
    if "data" in haystack or "python" in haystack:
        return "data"
    return "other"


def extract_skills(job_title: str, description: str, skill_keywords: list[str]) -> list[str]:
    text = f"{job_title} {description}".lower()
    found = []
    for skill in skill_keywords:
        if skill.lower() in text:
            found.append(skill)
    return sorted(set(found))


def find_first_value(row: dict[str, Any], candidates: list[str]) -> str:
    lowered = {clean_text(key).lower(): value for key, value in row.items()}
    for key in candidates:
        if key.lower() in lowered:
            return clean_text(lowered[key.lower()])
    return ""


def is_noise(company_name: str, job_title: str, description: str) -> str:
    if not company_name:
        return "missing_company_name"
    if not job_title:
        return "missing_job_title"
    if len(description) < 30:
        return "description_too_short"
    merged = " ".join([company_name, job_title, description])
    for pattern in NOISE_PATTERNS:
        if pattern.search(merged):
            return "noise_pattern_detected"
    return ""


def build_row_hash(company_name: str, job_title: str, city: str, description: str) -> str:
    payload = f"{company_name}|{job_title}|{city}|{description}"
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()[:16]


def confidence_score(record: NormalizedJobRecord) -> float:
    score = 0.0
    if record.company_name:
        score += 0.2
    if record.job_title:
        score += 0.2
    if len(record.job_description) >= 80:
        score += 0.2
    if record.city:
        score += 0.1
    if record.salary_min is not None:
        score += 0.1
    if record.education_level:
        score += 0.05
    if record.skills:
        score += min(0.15, 0.03 * len(record.skills))
    return round(min(score, 1.0), 4)


def mapping_candidates(field_mapping: dict[str, list[str]], key: str, fallback: list[str]) -> list[str]:
    return field_mapping.get(key, fallback)


def normalize_row(
    row: dict[str, Any],
    index: int,
    source_platform: str,
    field_mapping: dict[str, list[str]],
    city_aliases: dict[str, str],
    education_aliases: dict[str, str],
    skill_keywords: list[str],
) -> NormalizedJobRecord:
    company_name = find_first_value(row, mapping_candidates(field_mapping, "company_name", ["company_name", "company", "公司", "企业名称"]))
    job_title = find_first_value(row, mapping_candidates(field_mapping, "job_title", ["job_title", "title", "岗位名称", "职位名称", "职位"]))
    city = normalize_city(find_first_value(row, mapping_candidates(field_mapping, "city", ["city", "城市", "工作地点", "地点"])), city_aliases)
    salary_text = find_first_value(row, mapping_candidates(field_mapping, "salary_text", ["salary", "薪资", "薪资范围", "工资"]))
    education_level = normalize_education(find_first_value(row, mapping_candidates(field_mapping, "education_level", ["education", "学历", "学历要求"])), education_aliases)
    experience_text = find_first_value(row, mapping_candidates(field_mapping, "experience_text", ["experience", "经验", "工作经验", "经验要求"]))
    description = clean_text(find_first_value(row, mapping_candidates(field_mapping, "job_description", ["job_description", "description", "职位描述", "岗位职责", "职位详情"])))
    source_url = find_first_value(row, mapping_candidates(field_mapping, "source_url", ["source_url", "url", "链接", "职位链接"]))
    industry = find_first_value(row, mapping_candidates(field_mapping, "industry", ["industry", "行业", "所属行业"]))
    published_at = find_first_value(row, mapping_candidates(field_mapping, "published_at", ["published_at", "发布时间", "发布日期"]))

    skills = extract_skills(job_title, description, skill_keywords)
    category = infer_category(job_title, description, skills)
    salary_min, salary_max = parse_salary(salary_text)
    invalid_reason = is_noise(company_name, job_title, description)
    row_hash = build_row_hash(company_name, job_title, city, description)

    record = NormalizedJobRecord(
        source_id=find_first_value(row, mapping_candidates(field_mapping, "source_id", ["source_id", "id", "职位id", "岗位id"])) or f"{source_platform}-{index}",
        source_platform=source_platform,
        source_url=source_url,
        row_hash=row_hash,
        company_name=company_name,
        job_title=job_title,
        job_category=category,
        industry=industry,
        city=city,
        salary_text=salary_text,
        salary_min=salary_min,
        salary_max=salary_max,
        education_level=education_level,
        experience_text=experience_text,
        job_description=description,
        skills=skills,
        published_at=published_at,
        crawled_at=datetime.now().isoformat(timespec="seconds"),
        is_valid=not invalid_reason,
        invalid_reason=invalid_reason,
        confidence_score=0.0,
    )
    record.confidence_score = confidence_score(record)
    if record.is_valid and record.confidence_score < 0.45:
        record.invalid_reason = "low_confidence_manual_review"
    return record


def deduplicate(records: list[NormalizedJobRecord]) -> list[NormalizedJobRecord]:
    seen = set()
    deduped = []
    for record in records:
        unique_key = (record.company_name, record.job_title, record.city, record.row_hash)
        if unique_key in seen:
            if record.is_valid:
                record.is_valid = False
                record.invalid_reason = "duplicate_row"
            deduped.append(record)
            continue
        seen.add(unique_key)
        deduped.append(record)
    return deduped


def maybe_ai_review(records: list[NormalizedJobRecord]) -> None:
    # Reserved hook.
    # The intended behavior is to only send low-confidence rows to an
    # OpenAI-compatible model for structured review.
    #
    # This remains intentionally inactive by default because rule-based
    # preprocessing should stay the primary path.
    _ = records


def write_jsonl(path: Path, records: list[NormalizedJobRecord]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as f:
        for record in records:
            f.write(json.dumps(asdict(record), ensure_ascii=False) + "\n")


def write_summary(path: Path, records: list[NormalizedJobRecord]) -> None:
    valid = [r for r in records if r.is_valid]
    rejected = [r for r in records if not r.is_valid]
    review = [r for r in records if r.is_valid and r.confidence_score < 0.75]
    reasons = Counter(r.invalid_reason for r in rejected if r.invalid_reason)

    payload = {
        "total": len(records),
        "valid": len(valid),
        "rejected": len(rejected),
        "review": len(review),
        "top_rejection_reasons": reasons.most_common(10),
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def split_records(records: list[NormalizedJobRecord]) -> tuple[list[NormalizedJobRecord], list[NormalizedJobRecord], list[NormalizedJobRecord]]:
    valid = [r for r in records if r.is_valid]
    rejected = [r for r in records if not r.is_valid]
    review = [r for r in valid if r.confidence_score < 0.75]
    return valid, rejected, review


def ensure_output_dir(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def main() -> None:
    args = parse_args()
    input_path = Path(args.input)
    output_dir = Path(args.output_dir)
    ensure_output_dir(output_dir)
    field_mapping = load_field_mapping(args.mapping)
    city_aliases = load_json_map(args.city_aliases, CITY_ALIASES)
    education_aliases = load_json_map(args.education_aliases, EDUCATION_ALIASES)
    skill_keywords = load_skill_keywords(args.skill_keywords, SKILL_KEYWORDS)

    rows = read_rows(input_path, args.sheet_name)
    if args.sample_limit:
        rows = rows[: args.sample_limit]

    normalized = [
        normalize_row(
            row,
            index=index + 1,
            source_platform=args.source_platform,
            field_mapping=field_mapping,
            city_aliases=city_aliases,
            education_aliases=education_aliases,
            skill_keywords=skill_keywords,
        )
        for index, row in enumerate(rows)
    ]
    normalized = deduplicate(normalized)

    if args.ai_review:
        maybe_ai_review(normalized)

    valid, rejected, review = split_records(normalized)
    write_jsonl(output_dir / "normalized.jsonl", valid)
    write_jsonl(output_dir / "rejected.jsonl", rejected)
    write_jsonl(output_dir / "review.jsonl", review)
    write_summary(output_dir / "summary.json", normalized)

    print(json.dumps({
        "input": str(input_path),
        "output_dir": str(output_dir),
        "total_rows": len(rows),
        "valid_rows": len(valid),
        "rejected_rows": len(rejected),
        "review_rows": len(review),
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
