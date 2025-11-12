 package com.myproject.OAS.Controller;

import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.myproject.OAS.Model.Enquiry;
import com.myproject.OAS.Model.Users;
import com.myproject.OAS.Model.Users.UserRole;
import com.myproject.OAS.Model.Users.UserStatus;
import com.myproject.OAS.Repository.EnquiryRepository;
import com.myproject.OAS.Repository.UserRepository;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class Maincontroller {
	
	@Autowired
	private EnquiryRepository enquiryRepo;
	@Autowired
private UserRepository userRepo;
	
     @GetMapping("/")
     public String ShowIndex( )
     {
    	 return "index";   //index.html
     }
     
     @GetMapping("/Registration")
     public String ShowRegistration( HttpSession session, RedirectAttributes attributes, Model model)
     {
    	 if (session.getAttribute("NewStudent") == null) {
			return "redirect:/Login";
		}
    	 
    	 Users student = (Users) session.getAttribute("NewStudent");
    	 model.addAttribute("student", student);
    	 return "Registration";
     }
     
     @PostMapping("/Registration")
     public String completeRegistration(
             @RequestParam("fatherName") String fatherName,
             @RequestParam("motherName") String motherName,
             @RequestParam("gender") String gender,
             @RequestParam("address") String address,
             @RequestParam("utrNo") String utrNo,
             @RequestParam("file") MultipartFile file,
             HttpSession session,
             RedirectAttributes attributes) {
         
         try {
             // Session check
             if (session.getAttribute("NewStudent") == null) {
                 return "redirect:/Login";
             }
             
             Users existing = (Users) session.getAttribute("NewStudent");
             
             // ✅ Validation for existing fields
             if (existing.getName() == null || existing.getName().trim().isEmpty()) {
                 attributes.addFlashAttribute("msg", "Name is required!");
                 return "redirect:/Registration";
             }
             
             if (existing.getBranch() == null || existing.getBranch().trim().isEmpty()) {
                 attributes.addFlashAttribute("msg", "Branch is required!");
                 return "redirect:/Registration";
             }
             
             if (existing.getContactNo() == null || existing.getContactNo().trim().isEmpty()) {
                 attributes.addFlashAttribute("msg", "Contact Number is required!");
                 return "redirect:/Registration";
             }
             
             // ✅ Set new fields from form
             existing.setFatherName(fatherName);
             existing.setMotherName(motherName);
             existing.setGender(gender);
             existing.setAddress(address);
             existing.setUtrNo(utrNo);
             existing.setStatus(Users.UserStatus.PENDING);
             
             // ✅ File upload logic
             if (!file.isEmpty()) {
                 String storageFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                 String uploadDir = "Public/Payments/";
                 Path uploadPath = Paths.get(uploadDir);
                 
                 if (!Files.exists(uploadPath)) {
                     Files.createDirectories(uploadPath);
                 }
                 
                 try (InputStream inputStream = file.getInputStream()) {
                     Path filePath = uploadPath.resolve(storageFileName);
                     Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                 }
                 
                 existing.setPaymentImage(storageFileName);

             }
             
             userRepo.save(existing);
             attributes.addFlashAttribute("msg", "Registration submitted successfully! Waiting for approval.");
             session.removeAttribute("NewStudent");

             return "redirect:/Login";
         } catch (Exception e) {
             attributes.addFlashAttribute("msg", "Error: " + e.getMessage());
             return "redirect:/Registration";
         }
     }

     @GetMapping("/Successful")
     public String ShowSuccessful() 
     {
    	 return "Successful";
     }
     
     @GetMapping("/Login")
     public String ShowLogin( ) 
     {
    	 return "Login";
     }
     
     @PostMapping("/Login")
     public String Login(RedirectAttributes attributes, HttpServletRequest request, HttpSession session) 
     {
    try {
    	
    	String userType =request.getParameter("userType");
    	String userId = request.getParameter("username");
    	String password = request.getParameter("password");
    	
        if(userType.equals("ADMIN") && userRepo.existsByEmail(userId)) {
        	
           	Users admin = userRepo.findByEmail(userId);
           	if (password.equals(admin.getPassword( )) && admin.getRole().equals(UserRole.ADMIN)) {
           		
           		System.err.println("Valid Admin");
           		session.setAttribute("loggedInAdmin", admin);
           		return "redirect:/Admin/Dashboard";
           	}
           	else {
           		attributes.addFlashAttribute("msg", "Wrong User or Invalid Password");
           	}
        }else if (userType.equals("STUDENT") && userRepo.existsByRollNo(userId)) {
        	
        	Users student = userRepo.findByRollNo(userId);
           	if (password.equals(student.getPassword( )) && student.getRole().equals(UserRole.STUDENT)) {
           		
            if  (student.getStatus().equals(UserStatus.APPROVED)) {
            	  System.err.println("Valid Student");
            	  session.setAttribute("loggedInStudent", student);
            	  return "redirect:/Student/Dashboard";
            }
            else if(student.getStatus( ).equals(UserStatus.PENDING)) {
            	session.setAttribute("NewStudent", student);
            	return "redirect:/Registration";
            }
            else {
            	attributes.addFlashAttribute("msg", "Login Disabled, Please Contact Admin !") ;
            }
           	}
           	else {
           		attributes.addFlashAttribute("msg", "Wrong User or Invalid Password");
           	}
        }
        else {
        	attributes.addFlashAttribute("msg", "User not Found !!!!");
        }
    	
		return "redirect:/Login";
	} catch (Exception e) {
		attributes.addFlashAttribute("msg",  e.getMessage( ));
	return "redirect:/Login";
	}
     }
     
     @GetMapping ("/ContactUs")
     public String ShowContactUs( Model model) 
     {
    	 Enquiry enquiry = new Enquiry();
    	 model.addAttribute("enquiry", enquiry);
     return "contactus"; //contactus.html
     }
     
     @PostMapping("/ContactUs")
     public String ContactUs( @ModelAttribute("enquiry") Enquiry enquiry , RedirectAttributes attributes)
     {
    	 try {
    		 enquiry.setEnquiryDate(LocalDateTime.now( ));
    		 enquiryRepo.save(enquiry);
    		 attributes.addFlashAttribute("msg", "Enquiry Successfully Submitted !");
    		 
    		 return "redirect:/ContactUs";
		} catch (Exception e) {
			System.err.println("Error : "+e.getMessage());
		 return "redirect:/ContactUs";
		}
     }
}