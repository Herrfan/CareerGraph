export interface BasicInfo {
  name: string;
  education: string;
  major: string;
  school: string;
  graduation_year?: string;
}

export interface SoftAbilities {
  innovation: number;
  learning: number;
  stress_tolerance: number;
  communication: number;
  professional_skills?: number;
  certificates?: number;
  internship?: number;
}

export interface JobPreference {
  expected_position: string;
  expected_salary: string;
  expected_city: string;
}

export interface AbilityDescriptions {
  professional_skill?: string;
  soft_skill?: string;
  certificate?: string;
  innovation?: string;
  learning?: string;
  stress_tolerance?: string;
  communication?: string;
  internship?: string;
}

export interface InternshipExperience {
  company: string;
  position: string;
  period?: string;
  achievement?: string;
}

export interface ProjectExperience {
  name: string;
  role: string;
  description?: string;
  tech_stacks: string[];
  highlight?: string;
}

export interface StudentProfile {
  student_id: string;
  basic_info: BasicInfo;
  skills: string[];
  certificates: string[];
  soft_abilities: SoftAbilities;
  job_preference: JobPreference;
  ability_descriptions?: AbilityDescriptions;
  internship_experiences?: InternshipExperience[];
  project_experiences?: ProjectExperience[];
}

export interface StudentProfileEvaluation {
  completeness_score: number;
  competitiveness_score: number;
  dimensions: Record<string, number>;
}

export interface JobProfile {
  job_id: string;
  title: string;
  department: string;
  description: string;
  description_markdown?: string;
  category?: string;
  required_skills: string[];
  required_certificates: string[];
  ability_weights: Record<string, number>;
  ability_portrait?: Record<string, string>;
  ability_priority?: Record<string, number>;
  salary_range: string;
  city: string;
  experience_required: string;
  company_name?: string;
  work_address?: string;
  industry?: string;
  company_size?: string;
  company_type?: string;
  job_code?: string;
  company_intro?: string;
  portrait_scope?: string;
  city_tier?: string;
  salary_band?: string;
  portrait_source_count?: number;
  portrait_confidence?: number;
  source_record_ids?: string[];
  related_job_ids?: string[];
}

export interface JobOption {
  title: string;
  category?: string;
}

export interface JobSourceStats {
  total_records: number;
  active_records: number;
  using_enterprise_source: boolean;
  prefer_active_records: boolean;
  fallback_catalog_records: number;
}

export interface DimensionScores {
  basic_requirements?: number;
  professional_skills?: number;
  professional_quality?: number;
  growth_potential?: number;
  skills_match?: number;
  certificates_match?: number;
  soft_abilities_match?: number;
  salary_match?: number;
  city_match?: number;
}

export interface MatchResponse {
  match_score: number;
  dimension_scores: DimensionScores;
  matched_job?: JobProfile;
  recommendations?: string[];
}

export interface JobMatch {
  job: JobProfile;
  match_score: number;
  dimension_scores: DimensionScores;
}

export interface CategoryMatch {
  category: string;
  match_score: number;
  job_count?: number;
  recommend_count?: number;
}

export interface CareerPathNode {
  current?: string;
  level?: number;
  paths?: CareerPathNode[];
}

export interface CareerPathResponse {
  graph_paths?: CareerPathNode;
  career_path?: CareerPathNode;
  vector_similar_jobs?: Array<Record<string, unknown>>;
  similar_jobs?: Array<Record<string, unknown>>;
  vertical_path?: Array<Record<string, unknown>>;
  horizontal_path?: Array<Record<string, unknown>>;
}

export interface GraphSimilarJob {
  source_id: string;
  job_title: string;
  company_name: string;
  city: string;
  category: string;
  shared_skills: string[];
  similarity_score: number;
}

export interface GraphRoleContext {
  job_title: string;
  city_tier: string;
  salary_band: string;
  required_skills: string[];
  preferred_certificates: string[];
  top_abilities: string[];
  related_roles: string[];
  summary: string;
}

export interface SkillGapAnalysis {
  target_job: string;
  matched_skills: string[];
  missing_skills: string[];
  skill_match_score: number;
  summary: string;
}

export interface KnowledgeSnippet {
  snippet_id: string;
  title: string;
  source_type: string;
  source_id: string;
  content: string;
  keywords: string[];
  score: number;
}

export interface GrowthPlanSection {
  learning_path?: Array<Record<string, unknown>>;
  practice_arrangements?: Array<Record<string, unknown>>;
  evaluation_metrics?: Record<string, unknown>;
  learningPath?: Array<Record<string, unknown>>;
  practiceArrangements?: Array<Record<string, unknown>>;
  evaluationMetrics?: Record<string, unknown>;
}

export interface GrowthPlanResponse {
  success: boolean;
  short_term_plan?: GrowthPlanSection;
  mid_term_plan?: GrowthPlanSection;
  shortTermPlan?: GrowthPlanSection;
  midTermPlan?: GrowthPlanSection;
}

export interface GeneratedReportResponse {
  success: boolean;
  markdown_content: string;
}

export interface CompletenessCheckResponse {
  success: boolean;
  completeness_score: number;
  completed_items: string[];
  missing_items: string[];
  suggested_items: string[];
}

export interface ExportReportResponse {
  success: boolean;
  export_id: string;
  filename: string;
  format: string;
  content: string;
  content_type: string;
  available_sections: string[];
  metadata: Record<string, unknown>;
}

export interface ReportSnapshot {
  snapshot_id: string;
  student_id: string;
  target_job: string;
  matched_job_id?: string;
  matched_job_title?: string;
  markdown_content?: string;
  growth_plan?: GrowthPlanResponse | null;
  updated_at: string;
}

export interface ProfileScoring {
  completeness_score: number;
  competitiveness_score: number;
  completeness_breakdown: string[];
  competitiveness_breakdown: string[];
  target_job_title: string;
}

export interface AuthUser {
  id: number;
  username: string;
  role: string;
  is_guest: boolean;
}

export interface AuthResponse {
  success: boolean;
  token: string;
  user: AuthUser;
}

export interface AdminJobRecord {
  sourceId: string;
  companyName: string;
  jobTitle: string;
  jobCategory?: string;
  city?: string;
  industry?: string;
  salaryText?: string;
  experienceText?: string;
  educationLevel?: string;
  jobDescription?: string;
  skills?: string[];
  status?: string;
}

export interface StudentProfileSnapshot {
  snapshot_id: string;
  student_id: string;
  profile_data: StudentProfile;
  created_at: string;
}

export interface ChatMessageDTO {
  conversationKey: string;
  role: string;
  content: string;
  createdAt: string;
}
