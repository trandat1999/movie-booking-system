package com.moviebooking;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/** Kiểm tra quy tắc phân tầng backend (PLAN.md mục 2A.2). */
@AnalyzeClasses(packages = "com.moviebooking", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String MODULES_PACKAGE = "com.moviebooking.modules.";

    // L1: controller → service → repository, không đi tắt và không gọi ngược
    @ArchTest
    static final ArchRule controllersDoNotUseRepositories = noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule servicesDoNotDependOnControllers = noClasses()
            .that()
            .resideInAPackage("..service..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule repositoriesDoNotDependOnUpperLayers = noClasses()
            .that()
            .resideInAPackage("..repository..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..service..", "..controller..");

    // L2: controller không dùng entity
    @ArchTest
    static final ArchRule controllersDoNotExposeEntities = noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..entity..");

    // L3: transaction chỉ ở service/impl
    @ArchTest
    static final ArchRule transactionalMethodsOnlyInServiceImpl = methods()
            .that()
            .areAnnotatedWith(Transactional.class)
            .should()
            .beDeclaredInClassesThat()
            .resideInAPackage("..service.impl..");

    @ArchTest
    static final ArchRule transactionalClassesOnlyInServiceImpl = classes()
            .that()
            .areAnnotatedWith(Transactional.class)
            .should()
            .resideInAPackage("..service.impl..");

    @ArchTest
    static final ArchRule noJakartaTransactional = noClasses()
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("jakarta.transaction.Transactional")
            .because("dùng thống nhất org.springframework.transaction.annotation.Transactional");

    // L4: repository của module nào chỉ được dùng trong module đó
    @ArchTest
    static final ArchRule repositoriesOnlyUsedInsideOwnModule = classes()
            .that()
            .resideInAPackage("com.moviebooking.modules..repository..")
            .should(onlyBeAccessedFromOwnModule());

    // L5: infrastructure và các package dùng chung không phụ thuộc module nghiệp vụ
    @ArchTest
    static final ArchRule sharedPackagesIndependentOfModules = noClasses()
            .that()
            .resideInAnyPackage(
                    "com.moviebooking.infrastructure..", "com.moviebooking.common..", "com.moviebooking.exception..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.moviebooking.modules..");

    // L8: controller quản trị luôn có @PreAuthorize ở mức class
    @ArchTest
    static final ArchRule adminControllersAreSecured = classes()
            .that()
            .haveSimpleNameStartingWith("Admin")
            .and()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .beAnnotatedWith(PreAuthorize.class);

    private static ArchCondition<JavaClass> onlyBeAccessedFromOwnModule() {
        return new ArchCondition<>("only be accessed from their own module") {
            @Override
            public void check(JavaClass repository, ConditionEvents events) {
                String owner = moduleOf(repository);
                for (Dependency dependency : repository.getDirectDependenciesToSelf()) {
                    if (!Objects.equals(owner, moduleOf(dependency.getOriginClass()))) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static String moduleOf(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(MODULES_PACKAGE)) {
            return null;
        }
        String rest = packageName.substring(MODULES_PACKAGE.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
