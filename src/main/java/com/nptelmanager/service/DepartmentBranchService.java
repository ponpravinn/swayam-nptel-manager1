package com.nptelmanager.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Provides comprehensive options for Academic Departments and Degree Branches
 * for college students enrolling in SWAYAM NPTEL courses.
 */
@Service
public class DepartmentBranchService {

    private static final List<String> DEPARTMENTS = List.of(
        "Department of Computer Science & Engineering",
        "Department of Information Technology",
        "Department of Artificial Intelligence & Data Science",
        "Department of Electronics & Communication Engineering",
        "Department of Electrical & Electronics Engineering",
        "Department of Mechanical Engineering",
        "Department of Civil Engineering",
        "Department of Mechatronics & Automation",
        "Department of Chemical & Biotechnology",
        "Department of Biomedical Engineering",
        "Department of Aerospace & Aeronautical Engineering",
        "Department of Management Studies (MBA)",
        "Department of Computer Applications (MCA)",
        "Department of Science & Humanities",
        "Department of Arts & Commerce",
        "Other (Specify Custom Department)"
    );

    private static final List<String> BRANCHES = List.of(
        "Computer Science and Engineering (CSE)",
        "Information Technology (IT)",
        "Artificial Intelligence and Data Science (AI & DS)",
        "Artificial Intelligence and Machine Learning (AIML)",
        "Cyber Security",
        "Electronics and Communication Engineering (ECE)",
        "Electrical and Electronics Engineering (EEE)",
        "Mechanical Engineering (MECH)",
        "Civil Engineering (CIVIL)",
        "Mechatronics Engineering",
        "Robotics and Automation",
        "Biomedical Engineering",
        "Biotechnology (BT)",
        "Chemical Engineering",
        "Aerospace Engineering",
        "Aeronautical Engineering",
        "Automobile Engineering",
        "Master of Computer Applications (MCA)",
        "Master of Business Administration (MBA)",
        "M.E. / M.Tech Computer Science",
        "M.E. VLSI & Embedded Systems",
        "B.Sc Computer Science / IT",
        "B.Sc / M.Sc Mathematics / Physics / Chemistry",
        "B.Com / BBA",
        "Diploma in Engineering / Polytechnic",
        "Other (Specify Custom Branch)"
    );

    public List<String> getAllDepartments() {
        return DEPARTMENTS;
    }

    public List<String> getAllBranches() {
        return BRANCHES;
    }
}
