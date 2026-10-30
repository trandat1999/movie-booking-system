package com.moviebooking.seatengine;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Quy tắc L7: seat-engine không phụ thuộc framework. */
@AnalyzeClasses(packages = "com.moviebooking.seatengine", importOptions = ImportOption.DoNotIncludeTests.class)
class SeatEngineArchitectureTest {

    @ArchTest
    static final ArchRule noFrameworkDependencies = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..")
            .allowEmptyShould(true);
}
