package com.lms.Digital_library.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * DTO returned by resume parsing / analysis logic.
 * Contains parsed fields (name, contact, sections) plus
 * ATS score and suggestions for the candidate.
 */
public class ParsedResumeDto implements Serializable {

    private static final long serialVersionUID = 2L;

    // ---------- Nested Classes ----------

    public static class Experience implements Serializable {
        private static final long serialVersionUID = 1L;

        private String title;
        private String company;
        private String start;
        private String end;
        private List<String> bullets = new ArrayList<>();

        public Experience() { }

        public Experience(String title, String company, String start, String end) {
            this.title = title;
            this.company = company;
            this.start = start;
            this.end = end;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }

        public List<String> getBullets() { return bullets; }
        public void setBullets(List<String> bullets) {
            this.bullets = bullets == null ? new ArrayList<>() : bullets;
        }

        public void addBullet(String b) {
            if (b != null && !b.trim().isEmpty()) this.bullets.add(b.trim());
        }

        @Override
        public String toString() {
            return "Experience{" +
                    "title='" + title + '\'' +
                    ", company='" + company + '\'' +
                    ", start='" + start + '\'' +
                    ", end='" + end + '\'' +
                    ", bullets=" + bullets +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Experience)) return false;
            Experience that = (Experience) o;
            return Objects.equals(title, that.title) &&
                   Objects.equals(company, that.company) &&
                   Objects.equals(start, that.start) &&
                   Objects.equals(end, that.end) &&
                   Objects.equals(bullets, that.bullets);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, company, start, end, bullets);
        }
    }

    public static class Education implements Serializable {
        private static final long serialVersionUID = 1L;

        private String degree;
        private String school;
        private String year;
        private String grade; // CGPA or percentage

        public Education() { }

        public Education(String degree, String school, String year, String grade) {
            this.degree = degree;
            this.school = school;
            this.year = year;
            this.grade = grade;
        }

        public String getDegree() { return degree; }
        public void setDegree(String degree) { this.degree = degree; }

        public String getSchool() { return school; }
        public void setSchool(String school) { this.school = school; }

        public String getYear() { return year; }
        public void setYear(String year) { this.year = year; }

        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }

        @Override
        public String toString() {
            return "Education{" +
                    "degree='" + degree + '\'' +
                    ", school='" + school + '\'' +
                    ", year='" + year + '\'' +
                    ", grade='" + grade + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Education)) return false;
            Education that = (Education) o;
            return Objects.equals(degree, that.degree) &&
                   Objects.equals(school, that.school) &&
                   Objects.equals(year, that.year) &&
                   Objects.equals(grade, that.grade);
        }

        @Override
        public int hashCode() {
            return Objects.hash(degree, school, year, grade);
        }
    }

    // ---------- Top-level Parsed Fields ----------

    private String name;
    private String email;
    private String phone;
    private String location;
    private String careerObjective;
    private String summary;

    private List<String> skills = new ArrayList<>();
    private List<Experience> experience = new ArrayList<>();
    private List<Education> education = new ArrayList<>();
    private List<String> certifications = new ArrayList<>();
    private List<String> achievements = new ArrayList<>();
    private List<String> projects = new ArrayList<>();

    // ---------- Analysis Results ----------
    private int atsScore;
    private List<String> suggestions = new ArrayList<>();

    // ---------- Constructors ----------
    public ParsedResumeDto() { }

    // ---------- Getters / Setters ----------

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCareerObjective() { return careerObjective; }
    public void setCareerObjective(String careerObjective) { this.careerObjective = careerObjective; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) {
        this.skills = skills == null ? new ArrayList<>() : skills;
    }
    public void addSkill(String s) {
        if (s != null && !s.trim().isEmpty()) this.skills.add(s.trim());
    }

    public List<Experience> getExperience() { return experience; }
    public void setExperience(List<Experience> experience) {
        this.experience = experience == null ? new ArrayList<>() : experience;
    }
    public void addExperience(Experience e) {
        if (e != null) this.experience.add(e);
    }

    public List<Education> getEducation() { return education; }
    public void setEducation(List<Education> education) {
        this.education = education == null ? new ArrayList<>() : education;
    }
    public void addEducation(Education e) {
        if (e != null) this.education.add(e);
    }

    public List<String> getCertifications() { return certifications; }
    public void setCertifications(List<String> certifications) {
        this.certifications = certifications == null ? new ArrayList<>() : certifications;
    }
    public void addCertification(String c) {
        if (c != null && !c.trim().isEmpty()) this.certifications.add(c.trim());
    }

    public List<String> getAchievements() { return achievements; }
    public void setAchievements(List<String> achievements) {
        this.achievements = achievements == null ? new ArrayList<>() : achievements;
    }
    public void addAchievement(String a) {
        if (a != null && !a.trim().isEmpty()) this.achievements.add(a.trim());
    }

    public List<String> getProjects() { return projects; }
    public void setProjects(List<String> projects) {
        this.projects = projects == null ? new ArrayList<>() : projects;
    }
    public void addProject(String p) {
        if (p != null && !p.trim().isEmpty()) this.projects.add(p.trim());
    }

    public int getAtsScore() { return atsScore; }
    public void setAtsScore(int atsScore) { this.atsScore = atsScore; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions == null ? new ArrayList<>() : suggestions;
    }
    public void addSuggestion(String s) {
        if (s != null && !s.trim().isEmpty()) this.suggestions.add(s.trim());
    }

    @Override
    public String toString() {
        return "ParsedResumeDto{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", location='" + location + '\'' +
                ", careerObjective='" + careerObjective + '\'' +
                ", summary='" + summary + '\'' +
                ", skills=" + skills +
                ", experience=" + experience +
                ", education=" + education +
                ", certifications=" + certifications +
                ", achievements=" + achievements +
                ", projects=" + projects +
                ", atsScore=" + atsScore +
                ", suggestions=" + suggestions +
                '}';
    }
}
