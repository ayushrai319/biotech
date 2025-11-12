package com.myproject.OAS.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.myproject.OAS.Model.StudyMaterial;
import com.myproject.OAS.Model.StudyMaterial.MaterialType;
import java.util.List; 

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {

    long countByMaterialTypeAndProgramAndBranchAndYear(MaterialType assignment, String program, String branch, String year);

    List<StudyMaterial> findAllByMaterialTypeAndProgramAndBranchAndYear(
            MaterialType studyMaterial, String program, String branch, String year);

	long countByMaterialType(MaterialType assignment);
}
