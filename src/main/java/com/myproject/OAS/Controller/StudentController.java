package com.myproject.OAS.Controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.myproject.OAS.Model.StudyMaterial;
import com.myproject.OAS.Model.StudyMaterial.MaterialType;
import com.myproject.OAS.Model.Users;
import com.myproject.OAS.Repository.StudyMaterialRepository;
import com.myproject.OAS.Repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/Student")
public class StudentController {

    @Autowired
    private HttpSession session;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private StudyMaterialRepository materialRepo;

    // Dashboard
    @GetMapping("/Dashboard")
    public String ShowDashboard(Model model) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/Login";
        }

        Users student = (Users) session.getAttribute("loggedInStudent");

        // ✅ Filtered counts for dashboard cards
        long assignmentCount = materialRepo.countByMaterialTypeAndProgramAndBranchAndYear(
                MaterialType.ASSIGNMENT, student.getProgram(), student.getBranch(), student.getYear());
        long studyMaterialCount = materialRepo.countByMaterialTypeAndProgramAndBranchAndYear(
                MaterialType.STUDY_MATERIAL, student.getProgram(), student.getBranch(), student.getYear());

        // ✅ Fetch lists for tables
        List<StudyMaterial> assignments = materialRepo.findAllByMaterialTypeAndProgramAndBranchAndYear(
                MaterialType.ASSIGNMENT, student.getProgram(), student.getBranch(), student.getYear());
        List<StudyMaterial> studymaterials = materialRepo.findAllByMaterialTypeAndProgramAndBranchAndYear(
                MaterialType.STUDY_MATERIAL, student.getProgram(), student.getBranch(), student.getYear());

        model.addAttribute("student", student);
        model.addAttribute("assignmentCount", assignmentCount);
        model.addAttribute("studyMaterialCount", studyMaterialCount);
        model.addAttribute("assignments", assignments);
        model.addAttribute("studymaterials", studymaterials);

        return "Student/Dashboard";
    }

    // Study Material Page
    @GetMapping("/StudyMaterial")
    public String ShowStudyMaterial(Model model) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/Login";
        }

        Users student = (Users) session.getAttribute("loggedInStudent");

        List<StudyMaterial> studymaterials = materialRepo.findAllByMaterialTypeAndProgramAndBranchAndYear(
                MaterialType.STUDY_MATERIAL, student.getProgram(), student.getBranch(), student.getYear());

        model.addAttribute("studymaterials", studymaterials);

        return "Student/StudyMaterial";
    }

    // Assignment Page
    @GetMapping("/Assignment")
    public String ShowAssignment(Model model) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/Login";
        }

        Users student = (Users) session.getAttribute("loggedInStudent");

        List<StudyMaterial> assignments = materialRepo.findAllByMaterialTypeAndProgramAndBranchAndYear(
                MaterialType.ASSIGNMENT, student.getProgram(), student.getBranch(), student.getYear());

        model.addAttribute("assignments", assignments);

        return "Student/Assignment";
    }

    // View Profile
    @GetMapping("/ViewProfile")
    public String ShowViewProfile(Model model) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/Login";
        }

        Users student = (Users) session.getAttribute("loggedInStudent");
        model.addAttribute("student", student);

        return "Student/ViewProfile";
    }

    // Change Password Page
    @GetMapping("/ChangePassword")
    public String ShowChangePasswordPage() {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/Login";
        }
        return "Student/ChangePassword";
    }

    // Change Password Action
    @PostMapping("/ChangePassword")
    public String ChangePassword(HttpServletRequest request, RedirectAttributes attributes) {
        try {
            String oldPassword = request.getParameter("oldPassword");
            String newPassword = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");

            Users student = (Users) session.getAttribute("loggedInStudent");

            if (!newPassword.equals(confirmPassword)) {
                attributes.addFlashAttribute("msg", "New Password and Confirm Password are not same.");
                return "redirect:/Student/ChangePassword";
            }

            if (newPassword.equals(student.getPassword())) {
                attributes.addFlashAttribute("msg", "New Password and Old Password cannot be same.");
                return "redirect:/Student/ChangePassword";
            }

            if (oldPassword.equals(student.getPassword())) {
                student.setPassword(confirmPassword);
                userRepo.save(student);
                session.invalidate();
                attributes.addFlashAttribute("msg", "Password Successfully Changed!");
                return "redirect:/Login";
            } else {
                attributes.addFlashAttribute("msg", "Invalid Old Password!");
                return "redirect:/Student/ChangePassword";
            }

        } catch (Exception e) {
            attributes.addFlashAttribute("msg", "Error: " + e.getMessage());
            return "redirect:/Student/ChangePassword";
        }
    }

    // Logout
    @GetMapping("/logout")
    public String logout(RedirectAttributes attributes) {
        session.invalidate();
        attributes.addFlashAttribute("msg", "Successfully Logged Out!");
        return "redirect:/Login";
    }
}
