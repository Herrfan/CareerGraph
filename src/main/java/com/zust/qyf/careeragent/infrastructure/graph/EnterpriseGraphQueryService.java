package com.zust.qyf.careeragent.infrastructure.graph;

import com.zust.qyf.careeragent.domain.dto.graph.GraphRoleContextDTO;
import com.zust.qyf.careeragent.domain.dto.graph.GraphSimilarJobDTO;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EnterpriseGraphQueryService {
    private final Driver driver;

    public EnterpriseGraphQueryService(Driver driver) {
        this.driver = driver;
    }

    public List<GraphSimilarJobDTO> findSimilarJobs(String jobTitle, int limit) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> tx.run("""
                            MATCH (target:JobPosting)-[:REQUIRES]->(ts:Skill)
                            WHERE toLower(target.job_title) CONTAINS toLower($jobTitle)
                               OR toLower($jobTitle) CONTAINS toLower(target.job_title)
                            WITH target, collect(DISTINCT ts.name) AS targetSkills
                            LIMIT 1
                            MATCH (candidate:JobPosting)-[:REQUIRES]->(cs:Skill)
                            WHERE candidate.source_id <> target.source_id
                            OPTIONAL MATCH (company:Company)-[:POSTS]->(candidate)
                            WITH target, candidate, company, targetSkills, collect(DISTINCT cs.name) AS candidateSkills
                            WITH candidate, company, targetSkills, candidateSkills,
                                 [skill IN candidateSkills WHERE skill IN targetSkills] AS sharedSkills
                            WHERE size(sharedSkills) > 0
                            RETURN candidate.source_id AS sourceId,
                                   candidate.job_title AS jobTitle,
                                   coalesce(company.company_name, candidate.company_name) AS companyName,
                                   candidate.city AS city,
                                   candidate.job_category AS category,
                                   sharedSkills AS sharedSkills,
                                   round((toFloat(size(sharedSkills)) / toFloat(size(targetSkills))) * 100.0) / 100.0 AS similarityScore
                            ORDER BY similarityScore DESC, size(sharedSkills) DESC
                            LIMIT $limit
                            """, Values.parameters("jobTitle", jobTitle, "limit", Math.max(limit, 1)))
                    .list(this::mapSimilarJob));
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<String> loadRequiredSkills(String jobTitle) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> tx.run("""
                            MATCH (target:JobPosting)-[:REQUIRES]->(skill:Skill)
                            WHERE toLower(target.job_title) CONTAINS toLower($jobTitle)
                               OR toLower($jobTitle) CONTAINS toLower(target.job_title)
                            RETURN collect(DISTINCT skill.name) AS skills
                            LIMIT 1
                            """, Values.parameters("jobTitle", jobTitle))
                    .list(record -> record.get("skills").asList(value -> value.asString())))
                    .stream()
                    .findFirst()
                    .orElse(List.of());
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<GraphSimilarJobDTO> findSimilarPortraitRoles(String jobTitle, int limit) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> tx.run("""
                            MATCH (target:GenericRole)-[:REQUIRES]->(ts:Skill)
                            WHERE toLower(target.title) CONTAINS toLower($jobTitle)
                               OR toLower($jobTitle) CONTAINS toLower(target.title)
                            WITH target, collect(DISTINCT ts.name) AS targetSkills
                            LIMIT 1
                            MATCH (candidate:GenericRole)-[:REQUIRES]->(cs:Skill)
                            WHERE candidate.job_id <> target.job_id
                            OPTIONAL MATCH (target)-[rel:RELATED_TO]-(candidate)
                            WITH target, candidate, rel, targetSkills, collect(DISTINCT cs.name) AS candidateSkills
                            WITH candidate, rel, targetSkills, candidateSkills,
                                 [skill IN candidateSkills WHERE skill IN targetSkills] AS sharedSkills
                            WHERE size(sharedSkills) > 0
                            RETURN candidate.job_id AS sourceId,
                                   candidate.title AS jobTitle,
                                   coalesce(candidate.portrait_scope, 'generic_role') AS companyName,
                                   candidate.city_tier AS city,
                                   candidate.category AS category,
                                   sharedSkills AS sharedSkills,
                                   coalesce(rel.score, round((toFloat(size(sharedSkills)) / toFloat(case when size(targetSkills) = 0 then 1 else size(targetSkills) end)) * 100.0) / 100.0) AS similarityScore
                            ORDER BY similarityScore DESC, size(sharedSkills) DESC
                            LIMIT $limit
                            """, Values.parameters("jobTitle", jobTitle, "limit", Math.max(limit, 1)))
                    .list(this::mapSimilarJob));
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<String> loadPortraitRequiredSkills(String jobTitle) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> tx.run("""
                            MATCH (target:GenericRole)-[:REQUIRES]->(skill:Skill)
                            WHERE toLower(target.title) CONTAINS toLower($jobTitle)
                               OR toLower($jobTitle) CONTAINS toLower(target.title)
                            RETURN collect(DISTINCT skill.name) AS skills
                            LIMIT 1
                            """, Values.parameters("jobTitle", jobTitle))
                    .list(record -> record.get("skills").asList(value -> value.asString())))
                    .stream()
                    .findFirst()
                    .orElse(List.of());
        } catch (Exception e) {
            return List.of();
        }
    }

    public Optional<GraphRoleContextDTO> loadPortraitRoleContext(String jobTitle) {
        try (Session session = driver.session()) {
            Record role = session.executeRead(tx -> tx.run("""
                            MATCH (role:GenericRole)
                            WHERE toLower(role.title) CONTAINS toLower($jobTitle)
                               OR toLower($jobTitle) CONTAINS toLower(role.title)
                            OPTIONAL MATCH (role)-[:REQUIRES]->(skill:Skill)
                            OPTIONAL MATCH (role)-[:PREFERS_CERTIFICATE]->(certificate:Certificate)
                            OPTIONAL MATCH (role)-[abilityRel:VALUES_ABILITY]->(ability:AbilityDimension)
                            WITH role,
                                 collect(DISTINCT skill.name) AS skills,
                                 collect(DISTINCT certificate.name) AS certificates,
                                 collect(DISTINCT ability.label + '(' + toString(abilityRel.priority) + ')') AS abilities
                            RETURN role.title AS jobTitle,
                                   role.city_tier AS cityTier,
                                   role.salary_band AS salaryBand,
                                   skills AS skills,
                                   certificates AS certificates,
                                   abilities AS abilities
                            LIMIT 1
                            """, Values.parameters("jobTitle", jobTitle))
                    .stream()
                    .findFirst()
                    .orElse(null));

            if (role == null) {
                return Optional.empty();
            }

            List<String> relatedRoles = session.executeRead(tx -> tx.run("""
                            MATCH (role:GenericRole)
                            WHERE toLower(role.title) CONTAINS toLower($jobTitle)
                               OR toLower($jobTitle) CONTAINS toLower(role.title)
                            MATCH (role)-[rel:RELATED_TO]-(related:GenericRole)
                            RETURN related.title AS title
                            ORDER BY rel.score DESC
                            LIMIT 5
                            """, Values.parameters("jobTitle", jobTitle))
                    .list(record -> record.get("title").asString("")));

            List<String> skills = role.get("skills").asList(value -> value.asString()).stream().filter(value -> !value.isBlank()).toList();
            List<String> certificates = role.get("certificates").asList(value -> value.asString()).stream().filter(value -> !value.isBlank()).toList();
            List<String> abilities = role.get("abilities").asList(value -> value.asString()).stream().filter(value -> !value.isBlank()).toList();

            return Optional.of(new GraphRoleContextDTO(
                    role.get("jobTitle").asString(""),
                    role.get("cityTier").asString(""),
                    role.get("salaryBand").asString(""),
                    skills,
                    certificates,
                    abilities,
                    relatedRoles,
                    buildRoleSummary(role.get("jobTitle").asString(""), skills, certificates, abilities, relatedRoles)
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String buildRoleSummary(String jobTitle,
                                    List<String> skills,
                                    List<String> certificates,
                                    List<String> abilities,
                                    List<String> relatedRoles) {
        StringBuilder builder = new StringBuilder();
        builder.append("Role ").append(jobTitle).append(" focuses on skills ").append(String.join(", ", skills));
        if (!certificates.isEmpty()) {
            builder.append("; certificates ").append(String.join(", ", certificates));
        }
        if (!abilities.isEmpty()) {
            builder.append("; abilities ").append(String.join(", ", abilities));
        }
        if (!relatedRoles.isEmpty()) {
            builder.append("; related roles ").append(String.join(", ", relatedRoles));
        }
        return builder.toString();
    }

    private GraphSimilarJobDTO mapSimilarJob(Record record) {
        return new GraphSimilarJobDTO(
                record.get("sourceId").asString(""),
                record.get("jobTitle").asString(""),
                record.get("companyName").asString(""),
                record.get("city").asString(""),
                record.get("category").asString(""),
                record.get("sharedSkills").asList(value -> value.asString()),
                record.get("similarityScore").asDouble(0.0)
        );
    }
}
