# Data Pipeline Plan

## Goal

Replace the current manual `Excel -> JSON` workflow with a repeatable pipeline that supports:

- one-time preprocessing of large historical Excel files
- noise filtering and field normalization
- incremental updates from future Python crawlers
- dual storage in MySQL and Neo4j
- later retrieval by LangChain4j

## Recommended Architecture

Use three layers instead of reading raw files at runtime:

1. `raw`
Store the original Excel files, crawler HTML, and crawler JSON without modification.

2. `normalized`
Convert raw rows into a stable `JSONL` contract.
Each line should represent one cleaned job record.

3. `serving`
Import normalized records into:
- MySQL for detailed row storage and audit
- Neo4j for company/job/skill/city relationships

Runtime services should only read from the serving layer.

## Normalized Record Contract

Each normalized row should contain at least:

- `source_id`: stable external id or generated row id
- `source_platform`: source site name
- `source_url`: original job url
- `row_hash`: content hash used for dedup and incremental updates
- `company_name`
- `job_title`
- `job_category`
- `industry`
- `city`
- `salary_text`
- `salary_min`
- `salary_max`
- `education_level`
- `experience_text`
- `job_description`
- `skills`
- `published_at`
- `crawled_at`
- `is_valid`
- `invalid_reason`

## Noise Handling

Do noise filtering before import, not inside query logic.

Drop rows when:

- `company_name` is empty
- `job_title` is empty
- `job_description` is empty or too short
- the row is obvious ad/spam/template noise
- the same `row_hash` already exists in the current batch

Keep but downgrade rows when:

- salary cannot be parsed
- city cannot be standardized
- skill extraction is weak
- education or experience is missing

Recommended checks:

- text length threshold
- duplicate company + title + city + description hash
- invalid unicode / control characters cleanup
- salary parser with fallback to raw text
- city dictionary normalization
- company alias normalization
- skill dictionary extraction plus regex fallback

## Incremental Update Strategy

For crawler updates, do not compare full files.
Use `source_id` plus `row_hash`.

Rules:

- new `source_id`: insert
- same `source_id`, different `row_hash`: update
- same `source_id`, same `row_hash`: skip
- missing in latest crawl: mark inactive instead of hard delete

## Neo4j Model

Use a simple graph first:

- `(:Company {company_key, company_name, industry})`
- `(:JobPosting {source_id, job_title, row_hash, salary_text, education_level, experience_text, status})`
- `(:Skill {name})`
- `(:City {name})`
- `(:Category {name})`

Relations:

- `(:Company)-[:POSTS]->(:JobPosting)`
- `(:JobPosting)-[:REQUIRES]->(:Skill)`
- `(:JobPosting)-[:LOCATED_IN]->(:City)`
- `(:JobPosting)-[:BELONGS_TO]->(:Category)`

Add similarity edges later after enough clean data:

- `(:JobPosting)-[:SIMILAR_TO {score}]->(:JobPosting)`

## LangChain4j Direction

Do not start with a full agent.
Start with retrieval.

Priority order:

1. graph retrieval from Neo4j
2. structured lookup from MySQL
3. optional vector retrieval for long descriptions

Good first use cases:

- explain why a job is similar to another job
- explain which skills connect a student profile to a target job
- answer company/job questions grounded in graph data

## This Iteration

This repo should add:

- a normalized job record contract in Java
- a JSONL loader for normalized records
- a Neo4j importer service with `MERGE`
- an application service to orchestrate imports

Python preprocessing and crawler ingestion can then target the same contract.
