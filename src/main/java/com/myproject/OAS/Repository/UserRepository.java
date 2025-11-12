package com.myproject.OAS.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myproject.OAS.Model.Users;
import com.myproject.OAS.Model.Users.UserRole;
import com.myproject.OAS.Model.Users.UserStatus;

public interface UserRepository extends JpaRepository<Users, Long>{

	boolean existsByEmail(String userId);

	Users findByEmail(String userId);

	boolean existsByRollNo(String userId);

	Users findByRollNo(String userId);

	List<Users> findAllByRoleAndStatus(UserRole student, UserStatus pending);

	long countByRole(UserRole student);

	long countByRoleAndStatus(UserRole student, UserStatus pending);

	List<Users> findAllByRole(UserRole student);

	

}
