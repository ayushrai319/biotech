package com.myproject.OAS.Controller;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.myproject.OAS.Model.Enquiry;
import com.myproject.OAS.Model.StudyMaterial;
import com.myproject.OAS.Model.Users;
import com.myproject.OAS.Model.Users.UserRole;
import com.myproject.OAS.Model.Users.UserStatus;
import com.myproject.OAS.Repository.EnquiryRepository;
import com.myproject.OAS.Repository.StudyMaterialRepository;
import com.myproject.OAS.Repository.UserRepository;

import jakarta.mail.Multipart;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/Admin")
public class AdminController {
	
	@Autowired
	private HttpSession session;
	
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private EnquiryRepository enquiryRepo;
	
	@Autowired
	private StudyMaterialRepository materialRepo;

	@GetMapping("/Dashboard")
	public String ShowDashboard(Model model)
	{
		if (session.getAttribute("loggedInAdmin") == null) {
			return "redirect:/Login";
		}
		
		long assignmentCount = materialRepo.countByMaterialType(StudyMaterial.MaterialType.ASSIGNMENT);
		long studymaterialCount = materialRepo.countByMaterialType(StudyMaterial.MaterialType.STUDY_MATERIAL);
		long totalStudentCount = userRepo.countByRole(Users.UserRole.STUDENT);
		long pendingStudentCount = userRepo.countByRoleAndStatus(Users.UserRole.STUDENT, Users.UserStatus.PENDING);
		long enquiryCount = enquiryRepo.count();
		
		List<Enquiry> recentEnquiries = enquiryRepo.findTop5ByOrderByEnquiryDateDesc();
		
		//Add Attributes to model
		model.addAttribute("assignmentCount", assignmentCount);
		model.addAttribute("studymaterialCount", studymaterialCount);
		model.addAttribute("totalStudentCount", totalStudentCount);
		model.addAttribute("pendingStudentCount", pendingStudentCount); 
		model.addAttribute("enquiryCount", enquiryCount);
		model.addAttribute("recentEnquiries", recentEnquiries);
		
	    return "Admin/Dashboard";	
	}
	@GetMapping("/AddStudent") 
	public String ShowAddStudent(Model model) 
	{
		if (session.getAttribute("loggedInAdmin") == null) {
			return "redirect:/Login";
			}
		
		Users student = new Users();
		model.addAttribute("student", student);
		return "Admin/AddStudent";
	}
	
	@PostMapping("/AddStudent")
	public String AddStudent(@ModelAttribute("student") Users student, RedirectAttributes attributes) 
	{
		try {
			
			if (userRepo.existsByEmail(student.getEmail())) {
				attributes.addFlashAttribute("msg", "user already exists!!");
				return "redirect:/Admin/AddStudent";
			}
			
			student.setPassword("Password123");
			student.setRole(UserRole.STUDENT);
			student.setStatus(UserStatus.PENDING);
			student.setRollNo("E" + System.currentTimeMillis());   //Enrollment Number
			student.setRegDate(LocalDateTime.now());
			
			userRepo.save(student);
			attributes.addFlashAttribute("msg", "Registration Successful, Enrollment no : "+student.getRollNo()+", Password :  "+student.getPassword());
			return "redirect:/Admin/AddStudent";
			
		} catch (Exception e) {
			
			attributes.addFlashAttribute("msg", "Error : "+e.getMessage());
			return "redirect:/Admin/AddStudent";
		}
	}
	
	@GetMapping("/NewStudents")
	public String ShowNewStudents(Model model) 
	{
		if (session.getAttribute("loggedInAdmin") == null) {
		return "redirect:/Login";	
		}
		
		List<Users> newStudents = userRepo.findAllByRoleAndStatus(UserRole.STUDENT, UserStatus.PENDING);
		model.addAttribute("newStudents", newStudents);
		return "Admin/NewStudents";
	}
	
	 
	
	@GetMapping("/ManageStudents")
	public String ShowManageStudents(Model model) {
	    if (session.getAttribute("loggedInAdmin") == null) {
	        return "redirect:/Login";	
	    }

	    // All students with STUDENT role
	    List<Users> allStudents = userRepo.findAllByRole(UserRole.STUDENT);
	    model.addAttribute("newStudents", allStudents); // same variable as in template

	    return "Admin/ManageStudents";
	}

	
	
	@GetMapping("/UploadMaterial")
	public String ShowUploadMaterial(Model model)
	{
		if (session.getAttribute("loggedInAdmin") == null) {
			return "redirect:/Login";	
			}
		     StudyMaterial material = new StudyMaterial();
		     model.addAttribute("material", material);
	return "Admin/UploadMaterial";
	}
	
	@PostMapping("/UploadMaterial")
	public String uploadMaterial(
	        @ModelAttribute("material") StudyMaterial material,
	        @RequestParam("file") MultipartFile file,
	        RedirectAttributes attributes) {

	    try {
	        String storageFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
	        String uploadDir = "Public/StudyMaterial/";
	        Path uploadPath = Paths.get(uploadDir);

	        if (!Files.exists(uploadPath)) {
	            Files.createDirectories(uploadPath);
	        }

	        try (InputStream inputStream = file.getInputStream()) {
	            Path filePath = uploadPath.resolve(storageFileName);
	            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
	        }

	        material.setFileUrl(storageFileName);
	        material.setUploadedDate(LocalDateTime.now());

	        // ⚡ Fix: Admin upload ke liye student filter set karo
	        // Agar ye material specific program/branch/year ke liye hai
	        // material.setProgram("B.Tech");
	        // material.setBranch("CSE");
	        // material.setYear(2);

	        materialRepo.save(material);

	        attributes.addFlashAttribute("msg", material.getMaterialType().name() + " is uploaded successfully!");
	        return "redirect:/Admin/UploadMaterial";

	    } catch (Exception e) {
	        e.printStackTrace();
	        attributes.addFlashAttribute("msg", "Error: " + e.getMessage());
	        return "redirect:/Admin/UploadMaterial";
	    }
	}

	
	@GetMapping("/ApproveStudent")
	public String approveStudent(@RequestParam("id") long id, RedirectAttributes attributes) {
	    try {
	        Users student = userRepo.findById(id).orElse(null);
	        if (student != null && student.getStatus() == UserStatus.PENDING) {
	            student.setStatus(UserStatus.APPROVED);
	            userRepo.save(student);
	            attributes.addFlashAttribute("msg", "Student " + student.getName() + " approved successfully!");
	        } else {
	            attributes.addFlashAttribute("msg", "Student not found or already approved!");
	        }
	    } catch (Exception e) {
	        attributes.addFlashAttribute("msg", "Error: " + e.getMessage());
	    }
	    return "redirect:/Admin/NewStudents";
	}



	
	
	@GetMapping("/Enquiry") 
	public String ShowEnquiry (Model model) 
	{
		if (session.getAttribute("loggedInAdmin") == null) {
			return "redirect:/Login";
			}
		
		List<Enquiry> enquiries =  enquiryRepo.findAll();
		model.addAttribute("enquiries", enquiries);
		
		return "Admin/Enquiry";
	}
	
	@GetMapping("/DeleteEnquiry")
	public String DeleteEnquiry(@RequestParam("id") long id) 
	{
		enquiryRepo.deleteById(id);
		return "redirect:/Admin/Enquiry";
	}
	
	
	@GetMapping("/ChangePassword")
	public String ShowChangePasswordPage() {
	    if(session.getAttribute("loggedInAdmin") == null) {
	        return "redirect:/Login";
	    }
	    return "Admin/ChangePassword";
	}

	
	@PostMapping("/ChangePassword")
	public String ChangePassword(HttpServletRequest request, RedirectAttributes attributes)
	{
		try {
			
			
			String oldPassword = request.getParameter("oldPassword");
			String newPassword = request.getParameter("newPassword");
			String confirmPassword = request.getParameter("confirmPassword");
			
			if (! newPassword.equals(confirmPassword)) {
				attributes.addFlashAttribute("msg", "New Password and Confirm Password are not Same..");
				return "redirect:/Admin/ChangePassword";
			}
			Users admin = (Users) session.getAttribute("loggedInAdmin");
			
			if (newPassword.equals(admin.getPassword())) {
				attributes.addFlashAttribute("msg", "New Password and old Password can not be Same.");
				return "redirect:/Admin/ChangePassword";
			}
			
			if (oldPassword.equals(admin.getPassword())) {
				admin.setPassword(confirmPassword);
				userRepo.save(admin);
				session.invalidate();
				attributes.addFlashAttribute("msg", "Password Successfully Changed....");
				return "redirect:/Login";
			} 
			else {
				attributes.addFlashAttribute("msg", "Invalid Old Password!!!!!");
			}
			
			return "redirect:/Admin/ChangePassword";
		} catch (Exception e) {
			attributes.addFlashAttribute("msg", e.getMessage());
		      return "redirect:/Admin/ChangePassword";
		}
	}
	
	@GetMapping("/logout")
	public String logout(RedirectAttributes attributes)
	{
	session.invalidate()	;
	attributes.addFlashAttribute("msg", "Successfully Logged Out!!!!");
	return "redirect:/Login";
	}
	
}
