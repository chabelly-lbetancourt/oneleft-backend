package es.upm.miw.oneleft.users;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Checks the dependency rules of the hexagonal architecture:
 * the domain depends on nobody and the application does not depend on the infrastructure.
 */
@AnalyzeClasses(packages = "es.upm.miw.oneleft.users", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domainDoesNotDependOnOtherLayers = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domainDoesNotDependOnSpring = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .allowEmptyShould(true);

    /** The storage of the photos is only known by its adapter: S3 could be swapped without touching the rest */
    @ArchTest
    static final ArchRule onlyTheStorageAdapterKnowsTheAwsSdk = noClasses()
            .that().resideOutsideOfPackage("..infrastructure.storage..")
            .should().dependOnClassesThat().resideInAPackage("software.amazon..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationDoesNotDependOnInfrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .allowEmptyShould(true);
}
