package com.zust.qyf.careeragent.infrastructure.graph;

import com.zust.qyf.careeragent.domain.dto.importer.NormalizedJobRecord;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EnterpriseGraphService {
    private final Driver driver;

    public EnterpriseGraphService(Driver driver) {
        this.driver = driver;
    }

    public void ensureConstraints() {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run("CREATE CONSTRAINT company_key IF NOT EXISTS FOR (n:Company) REQUIRE n.company_key IS UNIQUE");
                tx.run("CREATE CONSTRAINT job_source_id IF NOT EXISTS FOR (n:JobPosting) REQUIRE n.source_id IS UNIQUE");
                tx.run("CREATE CONSTRAINT skill_name IF NOT EXISTS FOR (n:Skill) REQUIRE n.name IS UNIQUE");
                tx.run("CREATE CONSTRAINT city_name IF NOT EXISTS FOR (n:City) REQUIRE n.name IS UNIQUE");
                tx.run("CREATE CONSTRAINT category_name IF NOT EXISTS FOR (n:Category) REQUIRE n.name IS UNIQUE");
                tx.run("CREATE CONSTRAINT generic_role_id IF NOT EXISTS FOR (n:GenericRole) REQUIRE n.job_id IS UNIQUE");
                tx.run("CREATE CONSTRAINT certificate_name IF NOT EXISTS FOR (n:Certificate) REQUIRE n.name IS UNIQUE");
                tx.run("CREATE CONSTRAINT city_tier_name IF NOT EXISTS FOR (n:CityTier) REQUIRE n.name IS UNIQUE");
                tx.run("CREATE CONSTRAINT salary_band_name IF NOT EXISTS FOR (n:SalaryBand) REQUIRE n.name IS UNIQUE");
                tx.run("CREATE CONSTRAINT ability_dimension_key IF NOT EXISTS FOR (n:AbilityDimension) REQUIRE n.key IS UNIQUE");
                return null;
            });
        }
    }

    public long countJobPostings() {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> tx.run("""
                            MATCH (job:JobPosting)
                            RETURN count(job) AS total
                            """)
                    .single()
                    .get("total")
                    .asLong(0L));
        } catch (Exception e) {
            return -1L;
        }
    }

    public int importJobs(List<NormalizedJobRecord> records) {
        ensureConstraints();
        int imported = 0;
        try (Session session = driver.session()) {
            for (NormalizedJobRecord record : records) {
                if (!record.isImportable()) {
                    continue;
                }
                session.executeWrite(tx -> {
                    upsertRecord(tx, record);
                    return null;
                });
                imported++;
            }
        }
        return imported;
    }

    public long countGenericRoles() {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> tx.run("""
                            MATCH (role:GenericRole)
                            RETURN count(role) AS total
                            """)
                    .single()
                    .get("total")
                    .asLong(0L));
        } catch (Exception e) {
            return -1L;
        }
    }

    public int importPortraits(List<JobProfileDTO> jobs) {
        ensureConstraints();
        List<JobProfileDTO> validJobs = jobs.stream()
                .filter(job -> job != null && notBlank(job.jobId()) && notBlank(job.title()))
                .toList();

        try (Session session = driver.session()) {
            for (JobProfileDTO job : validJobs) {
                session.executeWrite(tx -> {
                    upsertPortrait(tx, job);
                    return null;
                });
            }
            session.executeWrite(tx -> {
                linkPortraitRelations(tx, validJobs);
                return null;
            });
        }
        return validJobs.size();
    }

    private void upsertRecord(TransactionContext tx, NormalizedJobRecord record) {
        tx.run("""
                MERGE (company:Company {company_key: $companyKey})
                SET company.company_name = $companyName,
                    company.industry = $industry

                MERGE (job:JobPosting {source_id: $sourceId})
                SET job.job_title = $jobTitle,
                    job.job_category = $jobCategory,
                    job.row_hash = $rowHash,
                    job.salary_text = $salaryText,
                    job.salary_min = $salaryMin,
                    job.salary_max = $salaryMax,
                    job.education_level = $educationLevel,
                    job.experience_text = $experienceText,
                    job.job_description = $jobDescription,
                    job.source_platform = $sourcePlatform,
                    job.source_url = $sourceUrl,
                    job.published_at = $publishedAt,
                    job.crawled_at = $crawledAt,
                    job.status = 'active'

                MERGE (company)-[:POSTS]->(job)
                """, Values.parameters(
                "companyKey", record.companyKey(),
                "companyName", record.companyName(),
                "industry", record.industry(),
                "sourceId", record.sourceId(),
                "jobTitle", record.jobTitle(),
                "jobCategory", record.jobCategory(),
                "rowHash", record.rowHash(),
                "salaryText", record.salaryText(),
                "salaryMin", record.salaryMin(),
                "salaryMax", record.salaryMax(),
                "educationLevel", record.educationLevel(),
                "experienceText", record.experienceText(),
                "jobDescription", record.jobDescription(),
                "sourcePlatform", record.sourcePlatform(),
                "sourceUrl", record.sourceUrl(),
                "publishedAt", record.publishedAt(),
                "crawledAt", record.crawledAt()
        ));

        if (record.cityKey() != null && !record.cityKey().isBlank()) {
            tx.run("""
                    MATCH (job:JobPosting {source_id: $sourceId})
                    MERGE (city:City {name: $city})
                    MERGE (job)-[:LOCATED_IN]->(city)
                    """, Values.parameters("sourceId", record.sourceId(), "city", record.city()));
        }

        if (record.categoryKey() != null && !record.categoryKey().isBlank()) {
            tx.run("""
                    MATCH (job:JobPosting {source_id: $sourceId})
                    MERGE (category:Category {name: $category})
                    MERGE (job)-[:BELONGS_TO]->(category)
                    """, Values.parameters("sourceId", record.sourceId(), "category", record.jobCategory()));
        }

        for (String skill : record.safeSkills()) {
            tx.run("""
                    MATCH (job:JobPosting {source_id: $sourceId})
                    MERGE (skill:Skill {name: $skill})
                    MERGE (job)-[:REQUIRES]->(skill)
                    """, Values.parameters("sourceId", record.sourceId(), "skill", skill));
        }
    }

    private void upsertPortrait(TransactionContext tx, JobProfileDTO job) {
        tx.run("""
                MERGE (role:GenericRole {job_id: $jobId})
                SET role.title = $title,
                    role.category = $category,
                    role.description = $description,
                    role.salary_range = $salaryRange,
                    role.city = $city,
                    role.experience_required = $experienceRequired,
                    role.portrait_scope = $portraitScope,
                    role.city_tier = $cityTier,
                    role.salary_band = $salaryBand,
                    role.portrait_source_count = $portraitSourceCount,
                    role.portrait_confidence = $portraitConfidence
                """, Values.parameters(
                "jobId", job.jobId(),
                "title", job.title(),
                "category", job.category(),
                "description", job.description(),
                "salaryRange", job.salaryRange(),
                "city", job.city(),
                "experienceRequired", job.experienceRequired(),
                "portraitScope", job.portraitScope(),
                "cityTier", job.cityTier(),
                "salaryBand", job.salaryBand(),
                "portraitSourceCount", job.portraitSourceCount(),
                "portraitConfidence", job.portraitConfidence()
        ));

        if (notBlank(job.category())) {
            tx.run("""
                    MATCH (role:GenericRole {job_id: $jobId})
                    MERGE (category:Category {name: $category})
                    MERGE (role)-[:BELONGS_TO]->(category)
                    """, Values.parameters("jobId", job.jobId(), "category", job.category()));
        }

        if (notBlank(job.cityTier())) {
            tx.run("""
                    MATCH (role:GenericRole {job_id: $jobId})
                    MERGE (cityTier:CityTier {name: $cityTier})
                    MERGE (role)-[:FITS_CITY_TIER]->(cityTier)
                    """, Values.parameters("jobId", job.jobId(), "cityTier", job.cityTier()));
        }

        if (notBlank(job.salaryBand())) {
            tx.run("""
                    MATCH (role:GenericRole {job_id: $jobId})
                    MERGE (salaryBand:SalaryBand {name: $salaryBand})
                    MERGE (role)-[:FITS_SALARY_BAND]->(salaryBand)
                    """, Values.parameters("jobId", job.jobId(), "salaryBand", job.salaryBand()));
        }

        for (String skill : safeList(job.requiredSkills())) {
            tx.run("""
                    MATCH (role:GenericRole {job_id: $jobId})
                    MERGE (skill:Skill {name: $skill})
                    MERGE (role)-[:REQUIRES]->(skill)
                    """, Values.parameters("jobId", job.jobId(), "skill", skill));
        }

        for (String certificate : safeList(job.requiredCertificates())) {
            tx.run("""
                    MATCH (role:GenericRole {job_id: $jobId})
                    MERGE (certificate:Certificate {name: $certificate})
                    MERGE (role)-[:PREFERS_CERTIFICATE]->(certificate)
                    """, Values.parameters("jobId", job.jobId(), "certificate", certificate));
        }

        for (Map.Entry<String, Integer> ability : safeMap(job.abilityPriority()).entrySet()) {
            tx.run("""
                    MATCH (role:GenericRole {job_id: $jobId})
                    MERGE (ability:AbilityDimension {key: $abilityKey})
                    SET ability.label = $abilityLabel
                    MERGE (role)-[rel:VALUES_ABILITY]->(ability)
                    SET rel.priority = $priority
                    """, Values.parameters(
                    "jobId", job.jobId(),
                    "abilityKey", ability.getKey(),
                    "abilityLabel", ability.getKey(),
                    "priority", ability.getValue()
            ));
        }
    }

    private void linkPortraitRelations(TransactionContext tx, List<JobProfileDTO> jobs) {
        for (int i = 0; i < jobs.size(); i++) {
            for (int j = i + 1; j < jobs.size(); j++) {
                JobProfileDTO left = jobs.get(i);
                JobProfileDTO right = jobs.get(j);
                List<String> sharedSkills = sharedSkills(left, right);
                double score = portraitRelationScore(left, right, sharedSkills);
                if (score < 0.28) {
                    continue;
                }

                tx.run("""
                        MATCH (left:GenericRole {job_id: $leftId})
                        MATCH (right:GenericRole {job_id: $rightId})
                        MERGE (left)-[rel:RELATED_TO]->(right)
                        SET rel.score = $score,
                            rel.shared_skills = $sharedSkills,
                            rel.reason = $reason
                        """, Values.parameters(
                        "leftId", left.jobId(),
                        "rightId", right.jobId(),
                        "score", score,
                        "sharedSkills", sharedSkills,
                        "reason", buildRelationReason(left, right, sharedSkills)
                ));
            }
        }
    }

    private List<String> sharedSkills(JobProfileDTO left, JobProfileDTO right) {
        return safeList(left.requiredSkills()).stream()
                .filter(skill -> safeList(right.requiredSkills()).stream()
                        .anyMatch(candidate -> normalize(candidate).equals(normalize(skill))))
                .distinct()
                .toList();
    }

    private double portraitRelationScore(JobProfileDTO left, JobProfileDTO right, List<String> sharedSkills) {
        double score = 0.0;
        if (normalize(left.category()).equals(normalize(right.category()))) {
            score += 0.32;
        }
        if (normalize(left.cityTier()).equals(normalize(right.cityTier()))) {
            score += 0.08;
        }
        if (normalize(left.salaryBand()).equals(normalize(right.salaryBand()))) {
            score += 0.08;
        }
        int base = Math.max(1, Math.max(safeList(left.requiredSkills()).size(), safeList(right.requiredSkills()).size()));
        score += sharedSkills.size() * 0.55 / base;
        return round(score);
    }

    private String buildRelationReason(JobProfileDTO left, JobProfileDTO right, List<String> sharedSkills) {
        if (!sharedSkills.isEmpty()) {
            return "shared_skills:" + String.join(",", sharedSkills);
        }
        if (normalize(left.category()).equals(normalize(right.category()))) {
            return "same_category";
        }
        return "portrait_affinity";
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(this::notBlank).toList();
    }

    private Map<String, Integer> safeMap(Map<String, Integer> values) {
        return values == null ? Map.of() : values;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").replace("/", "").replace("+", "");
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
