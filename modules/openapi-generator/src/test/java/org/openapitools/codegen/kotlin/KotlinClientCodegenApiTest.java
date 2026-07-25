package org.openapitools.codegen.kotlin;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.jetbrains.annotations.NotNull;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.languages.KotlinClientCodegen;
import org.openapitools.codegen.languages.features.CXFServerFeatures;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.openapitools.codegen.TestUtils.assertFileContains;
import static org.openapitools.codegen.TestUtils.assertFileNotContains;

public class KotlinClientCodegenApiTest {

    @DataProvider(name = "clientLibraries")
    public Object[][] pathResponses() {
        return new Object[][]{
                {ClientLibrary.JVM_KTOR},
                {ClientLibrary.JVM_OKHTTP4},
                {ClientLibrary.JVM_SPRING_WEBCLIENT},
                {ClientLibrary.JVM_SPRING_RESTCLIENT},
                {ClientLibrary.JVM_RETROFIT2},
                {ClientLibrary.MULTIPLATFORM},
                {ClientLibrary.JVM_VOLLEY},
                {ClientLibrary.JVM_VERTX}
        };
    }

    @Test(dataProvider = "clientLibraries")
    void testPathVariableIsNotEscaped_19930(ClientLibrary library) throws IOException {

        OpenAPI openAPI = readOpenAPI("src/test/resources/3_0/kotlin/issue19930-path-escaping.json");

        KotlinClientCodegen codegen = createCodegen(library);

        String outputPath = codegen.getOutputDir().replace('\\', '/');
        ClientOptInput input = createClientOptInput(openAPI, codegen);

        DefaultGenerator generator = new DefaultGenerator();

        enableOnlyApiGeneration(generator);

        generator.opts(input).generate();

        System.out.println(outputPath);

        assertFileContains(Paths.get(outputPath + "/src/" + library.getSourceRoot() + "/org/openapitools/client/apis/ArticleApi.kt"), "article('{Id}')");
    }

    @DataProvider(name = "useResponseAsReturnType")
    public static Object[][] useResponseAsReturnTypeTestData() {
        return new Object[][]{
                {null, "Response<Pet>", ": Response<Unit>"},
                {true, "Response<Pet>", ": Response<Unit>"},
                {false, "Pet", ""},
                {"false", "Pet", ""}};
    }

    @DataProvider(name = "librariesWithDateQueryHelper")
    public static Object[][] librariesWithDateQueryHelper() {
        return new Object[][]{
                {ClientLibrary.JVM_OKHTTP4},
                {ClientLibrary.JVM_SPRING_WEBCLIENT},
                {ClientLibrary.JVM_SPRING_RESTCLIENT},
                {ClientLibrary.JVM_VERTX}
        };
    }

    @Test(dataProvider = "useResponseAsReturnType")
    public void testUseResponseAsReturnType(Object useResponseAsReturnType, String expectedResponse, String expectedUnitResponse) throws IOException {
        OpenAPI openAPI = readOpenAPI("3_0/kotlin/petstore.yaml");

        KotlinClientCodegen codegen = createCodegen(ClientLibrary.JVM_RETROFIT2);
        codegen.additionalProperties().put(KotlinClientCodegen.USE_COROUTINES, "true");
        if (useResponseAsReturnType != null) {
            codegen.additionalProperties().put(KotlinClientCodegen.USE_RESPONSE_AS_RETURN_TYPE, useResponseAsReturnType);
        }

        ClientOptInput input = createClientOptInput(openAPI, codegen);

        DefaultGenerator generator = new DefaultGenerator();

        enableOnlyApiGeneration(generator);

        List<File> files = generator.opts(input).generate();
        File petApi = files.stream().filter(file -> file.getName().equals("PetApi.kt")).findAny().orElseThrow();
        List<String> lines = Files.readAllLines(petApi.toPath()).stream().map(String::trim).collect(Collectors.toList());
        assertFileContainsLine(lines, "suspend fun addPet(@Body pet: Pet): " + expectedResponse);
        assertFileContainsLine(lines, "suspend fun deletePet(@Path(\"petId\") petId: kotlin.Long, @Header(\"api_key\") apiKey: kotlin.String? = null)" + expectedUnitResponse);
    }

    @Test
    public void testEnumDefaultForReferencedSchemaParameterJvmOkhttp4() throws IOException {
        OpenAPI openAPI = readOpenAPI("3_0/kotlin/enum-default-query.yaml");

        KotlinClientCodegen codegen = createCodegen(ClientLibrary.JVM_OKHTTP4);
        codegen.additionalProperties().put("enumPropertyNaming", "UPPERCASE");

        ClientOptInput input = createClientOptInput(openAPI, codegen);

        DefaultGenerator generator = new DefaultGenerator();
        enableOnlyApiGeneration(generator);

        List<File> files = generator.opts(input).generate();
        File statusApi = files.stream().filter(file -> file.getName().equals("StatusApi.kt")).findAny().orElseThrow();

        assertFileContains(statusApi.toPath(), "state: PetStatus? = PetStatus.AVAILABLE");
    }

    @Test(dataProvider = "clientLibraries")
    void testEnumReservedDefaultNotHtmlEscaped(ClientLibrary library) throws IOException {
        OpenAPI openAPI = readOpenAPI("src/test/resources/3_0/kotlin/enum-default-query-reserved-word.json");
        KotlinClientCodegen codegen = createCodegen(library);
        ClientOptInput input = createClientOptInput(openAPI, codegen);
        DefaultGenerator generator = new DefaultGenerator();
        enableOnlyApiGeneration(generator);

        List<File> files = generator.opts(input).generate();
        File documentApiFile = files.stream().filter(file -> file.getName().equals("DocumentApi.kt")).findAny().orElseThrow();

        String documentApiContents = Files.readString(documentApiFile.toPath());
        if (!documentApiContents.contains("enum class")) {
            return;
        }

        String expectedEnumName = "DispositionDocumentDownload";
        if (!documentApiContents.contains("enum class " + expectedEnumName)) {
            Assert.fail("Kotlin client library " + library.getLibraryName() + " generated enum class name for an operation parameter has changed. Please update the 'expectedEnumName' in this test to match the new name.");
        }

        assertFileContains(documentApiFile.toPath(), "disposition: " + expectedEnumName + "? = DispositionDocumentDownload.`inline`");
    }

    @Test
    public void testJvmOkHttp4ApiClientUsesExplicitDateTypeArgumentsForQuerySerialization() throws IOException {
        OpenAPI openAPI = readOpenAPI("3_0/kotlin/petstore.yaml");

        KotlinClientCodegen codegen = createCodegen(ClientLibrary.JVM_OKHTTP4);
        String outputPath = codegen.getOutputDir().replace('\\', '/');

        DefaultGenerator generator = new DefaultGenerator();
        generator.setGeneratorPropertyDefault(CodegenConstants.MODELS, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.MODEL_TESTS, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.MODEL_DOCS, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.APIS, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.API_TESTS, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.API_DOCS, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.SUPPORTING_FILES, "true");

        generator.opts(createClientOptInput(openAPI, codegen)).generate();

        String apiClientPath = outputPath + "/src/main/kotlin/org/openapitools/client/infrastructure/ApiClient.kt";
        assertFileContains(Paths.get(apiClientPath), "is OffsetDateTime -> parseDateToQueryString<OffsetDateTime>(value)");
        assertFileContains(Paths.get(apiClientPath), "is OffsetTime -> parseDateToQueryString<OffsetTime>(value)");
        assertFileContains(Paths.get(apiClientPath), "is LocalDateTime -> parseDateToQueryString<LocalDateTime>(value)");
        assertFileContains(Paths.get(apiClientPath), "is LocalDate -> parseDateToQueryString<LocalDate>(value)");
        assertFileContains(Paths.get(apiClientPath), "is LocalTime -> parseDateToQueryString<LocalTime>(value)");
        assertFileNotContains(Paths.get(apiClientPath), "is OffsetDateTime -> parseDateToQueryString(value)");
    }

    @Test(dataProvider = "librariesWithDateQueryHelper")
    public void testGeneratedApisUseExplicitDateTypeArgumentsForQuerySerialization(ClientLibrary library) throws IOException {
        OpenAPI openAPI = readOpenAPI("3_0/kotlin/echo_api.yaml");

        KotlinClientCodegen codegen = createCodegen(library);
        DefaultGenerator generator = new DefaultGenerator();

        enableOnlyApiGeneration(generator);

        List<File> files = generator.opts(createClientOptInput(openAPI, codegen)).generate();
        File queryApi = files.stream().filter(file -> file.getName().equals("QueryApi.kt")).findAny().orElseThrow();

        assertFileContains(queryApi.toPath(), "parseDateToQueryString<kotlin.time.Instant>(");
        assertFileContains(queryApi.toPath(), "parseDateToQueryString<kotlinx.datetime.LocalDate>(");
        assertFileNotContains(queryApi.toPath(), "parseDateToQueryString(datetimeQuery)");
        assertFileNotContains(queryApi.toPath(), "parseDateToQueryString(dateQuery)");
        assertFileNotContains(queryApi.toPath(), "parseDateToQueryString(it)");
    }

    @Test
    public void testJvmKtorQueryParamWithTypeObject() throws IOException {
        OpenAPI openAPI = readOpenAPI("3_0/kotlin/jvm-ktor-type-object-query.yaml");

        KotlinClientCodegen codegen = createCodegen(ClientLibrary.JVM_KTOR);
        DefaultGenerator generator = new DefaultGenerator();
        enableOnlyApiGeneration(generator);

        List<File> files = generator.opts(createClientOptInput(openAPI, codegen)).generate();
        File defaultApi = files.stream().filter(file -> file.getName().equals("DefaultApi.kt")).findAny().orElseThrow();

        assertFileContains(defaultApi.toPath(), "mapFormExplode?.forEach { (key, value) -> localVariableQuery[key]");
        assertFileContains(defaultApi.toPath(), "mapFormNoexplode?.takeIf");
        assertFileContains(defaultApi.toPath(), "localVariableQuery[\"map_deep[$key]\"]");

        assertFileContains(defaultApi.toPath(), "modelFormExplode?.a?.let { localVariableQuery[\"a\"]");
        assertFileContains(defaultApi.toPath(), "modelFormNoexplode?.let { _model -> listOfNotNull(_model.a?.let { \"a,$it\" }, _model.b?.let { \"b,$it\" })");
        assertFileContains(defaultApi.toPath(), "localVariableQuery[\"model_deep[a]\"]");

        assertFileNotContains(defaultApi.toPath(), "mapDeep?.apply {");
    }

    @Test(description = "oneOf wrappers with a discriminator fall back to UnknownDefaultOpenApi when oneOfUnknownDefaultCase is enabled")
    public void testOneOfDiscriminatorUnknownDefaultCase() throws IOException {
        OpenAPI openAPI = readOpenAPI("src/test/resources/3_0/kotlin/polymorphism-oneof-discriminator.yaml");

        KotlinClientCodegen codegen = createOneOfWrappersCodegen("kotlinx_serialization", true);

        DefaultGenerator generator = new DefaultGenerator();
        enableOnlyModelGeneration(generator);

        List<File> files = generator.opts(createClientOptInput(openAPI, codegen)).generate();
        File animal = generatedFile(files, "Animal.kt");

        assertFileContains(animal.toPath(), "value class UnknownDefaultOpenApi(val value: JsonObject) : Animal");
        assertFileContains(animal.toPath(), "is Animal.UnknownDefaultOpenApi -> value.value");
        assertFileContains(animal.toPath(), "else -> Animal.UnknownDefaultOpenApi(element)");
        assertFileNotContains(animal.toPath(), "else -> throw SerializationException(\"Unknown Animal");
    }

    @Test(description = "oneOf wrappers without a discriminator fall back to UnknownDefaultOpenApi when oneOfUnknownDefaultCase is enabled")
    public void testOneOfNonDiscriminatorUnknownDefaultCase() throws IOException {
        OpenAPI openAPI = readOpenAPI("src/test/resources/3_0/kotlin/oneof-anyof-non-discriminator.yaml");

        KotlinClientCodegen codegen = createOneOfWrappersCodegen("kotlinx_serialization", true);

        DefaultGenerator generator = new DefaultGenerator();
        enableOnlyModelGeneration(generator);

        List<File> files = generator.opts(createClientOptInput(openAPI, codegen)).generate();
        File userOrPet = generatedFile(files, "UserOrPet.kt");

        assertFileContains(userOrPet.toPath(), "value class UnknownDefaultOpenApi(val value: JsonElement) : UserOrPet");
        assertFileContains(userOrPet.toPath(), "is UserOrPet.UnknownDefaultOpenApi -> jsonEncoder.encodeJsonElement(value.value)");
        assertFileContains(userOrPet.toPath(), "return UserOrPet.UnknownDefaultOpenApi(jsonElement)");
        assertFileNotContains(userOrPet.toPath(), "throw SerializationException(\"Cannot deserialize UserOrPet");

        // anyOf wrappers are not covered by the option
        assertFileNotContains(generatedFile(files, "AnyOfUserOrPet.kt").toPath(), "UnknownDefaultOpenApi");
    }

    @Test(description = "oneOf wrappers keep throwing on unknown variants when oneOfUnknownDefaultCase is disabled")
    public void testOneOfUnknownDefaultCaseDisabledByDefault() throws IOException {
        OpenAPI openAPI = readOpenAPI("src/test/resources/3_0/kotlin/polymorphism-oneof-discriminator.yaml");

        KotlinClientCodegen codegen = createOneOfWrappersCodegen("kotlinx_serialization", false);

        DefaultGenerator generator = new DefaultGenerator();
        enableOnlyModelGeneration(generator);

        List<File> files = generator.opts(createClientOptInput(openAPI, codegen)).generate();
        File animal = generatedFile(files, "Animal.kt");

        assertFileContains(animal.toPath(), "else -> throw SerializationException(\"Unknown Animal discriminator: $discriminatorValue\")");
        assertFileNotContains(animal.toPath(), "UnknownDefaultOpenApi");
    }

    @Test(description = "oneOfUnknownDefaultCase is ignored for serialization libraries without kotlinx oneOf wrappers")
    public void testOneOfUnknownDefaultCaseIgnoredForUnsupportedSerializationLibrary() throws IOException {
        OpenAPI openAPI = readOpenAPI("src/test/resources/3_0/kotlin/oneof-anyof-non-discriminator.yaml");

        KotlinClientCodegen codegen = createOneOfWrappersCodegen("gson", true);

        DefaultGenerator generator = new DefaultGenerator();
        enableOnlyModelGeneration(generator);

        List<File> files = generator.opts(createClientOptInput(openAPI, codegen)).generate();

        assertFileNotContains(generatedFile(files, "UserOrPet.kt").toPath(), "UnknownDefaultOpenApi");
    }

    private KotlinClientCodegen createOneOfWrappersCodegen(String serializationLibrary, boolean oneOfUnknownDefaultCase) throws IOException {
        KotlinClientCodegen codegen = createCodegen(ClientLibrary.JVM_RETROFIT2);
        codegen.setSerializationLibrary(serializationLibrary);
        codegen.additionalProperties().put(KotlinClientCodegen.GENERATE_ONEOF_ANYOF_WRAPPERS, true);
        if (oneOfUnknownDefaultCase) {
            codegen.additionalProperties().put(KotlinClientCodegen.ONE_OF_UNKNOWN_DEFAULT_CASE, true);
        }
        return codegen;
    }

    private static File generatedFile(List<File> files, String name) {
        return files.stream().filter(file -> file.getName().equals(name)).findAny().orElseThrow();
    }

    private static void enableOnlyModelGeneration(DefaultGenerator generator) {
        generator.setGeneratorPropertyDefault(CodegenConstants.MODELS, "true");
        generator.setGeneratorPropertyDefault(CodegenConstants.MODEL_TESTS, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.MODEL_DOCS, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.APIS, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.SUPPORTING_FILES, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.API_TESTS, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.API_DOCS, "false");
    }

    @Test(description = "oneOf deserializer rejects variants that only decoded because of the unknown default enum case")
    public void testOneOfEnumUnknownDefaultCaseGuard() throws IOException {
        File output = Files.createTempDirectory("test").toFile().getCanonicalFile();
        output.deleteOnExit();

        final CodegenConfigurator configurator = new CodegenConfigurator()
                .setGeneratorName("kotlin")
                .setLibrary(ClientLibrary.JVM_RETROFIT2.getLibraryName())
                .setInputSpec("src/test/resources/3_0/kotlin/oneof-anyof-non-discriminator.yaml")
                .setOutputDir(output.getAbsolutePath())
                .addAdditionalProperty(CodegenConstants.SERIALIZATION_LIBRARY, "kotlinx_serialization")
                .addAdditionalProperty(KotlinClientCodegen.GENERATE_ONEOF_ANYOF_WRAPPERS, true)
                .addAdditionalProperty("enumUnknownDefaultCase", true);

        List<File> files = new DefaultGenerator().opts(configurator.toClientOptInput()).generate();

        File oneOfModel = generatedFile(files, "UserOrPet.kt");
        assertFileContains(oneOfModel.toPath(), "import org.openapitools.client.infrastructure.containsUnknownDefaultOpenApiCase");
        assertFileContains(oneOfModel.toPath(), "require(!instance.containsUnknownDefaultOpenApiCase())");

        File petModel = generatedFile(files, "Pet.kt");
        assertFileContains(petModel.toPath(), ": org.openapitools.client.infrastructure.UnknownCaseCheckable");
        assertFileContains(petModel.toPath(), "override val containsUnknownDefaultOpenApiCase: kotlin.Boolean");
        assertFileContains(petModel.toPath(), "if (status.containsUnknownDefaultOpenApiCase()) return true");
        assertFileContains(petModel.toPath(), "if (kind.containsUnknownDefaultOpenApiCase()) return true");

        File petKindModel = generatedFile(files, "PetKind.kt");
        assertFileContains(petKindModel.toPath(), ": org.openapitools.client.infrastructure.UnknownCaseCheckable {");
        assertFileContains(petKindModel.toPath(), "override val containsUnknownDefaultOpenApiCase: kotlin.Boolean");

        File checkable = generatedFile(files, "UnknownCaseCheckable.kt");
        assertFileContains(checkable.toPath(), "interface UnknownCaseCheckable");
        assertFileContains(checkable.toPath(), "internal fun kotlin.Any?.containsUnknownDefaultOpenApiCase(): kotlin.Boolean");

        // models without eligible enum properties must not implement the interface
        assertFileNotContains(generatedFile(files, "User.kt").toPath(), "UnknownCaseCheckable");
    }

    @Test(description = "oneOf deserializer is unchanged when enumUnknownDefaultCase is disabled")
    public void testOneOfWithoutEnumUnknownDefaultCaseHasNoGuard() throws IOException {
        File output = Files.createTempDirectory("test").toFile().getCanonicalFile();
        output.deleteOnExit();

        final CodegenConfigurator configurator = new CodegenConfigurator()
                .setGeneratorName("kotlin")
                .setLibrary(ClientLibrary.JVM_RETROFIT2.getLibraryName())
                .setInputSpec("src/test/resources/3_0/kotlin/oneof-anyof-non-discriminator.yaml")
                .setOutputDir(output.getAbsolutePath())
                .addAdditionalProperty(CodegenConstants.SERIALIZATION_LIBRARY, "kotlinx_serialization")
                .addAdditionalProperty(KotlinClientCodegen.GENERATE_ONEOF_ANYOF_WRAPPERS, true);

        List<File> files = new DefaultGenerator().opts(configurator.toClientOptInput()).generate();

        assertFileNotContains(generatedFile(files, "UserOrPet.kt").toPath(), "containsUnknownDefaultOpenApiCase");
        assertFileNotContains(generatedFile(files, "Pet.kt").toPath(), "UnknownCaseCheckable");

        Assert.assertTrue(files.stream().noneMatch(f -> f.getName().equals("UnknownCaseCheckable.kt")),
                "UnknownCaseCheckable.kt should not be generated when enumUnknownDefaultCase is disabled");
    }

    private static void assertFileContainsLine(List<String> lines, String line) {
        Assert.assertListContains(lines, s -> s.equals(line), line);
    }

    private static void enableOnlyApiGeneration(DefaultGenerator generator) {
        generator.setGeneratorPropertyDefault(CodegenConstants.MODELS, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.MODEL_TESTS, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.MODEL_DOCS, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.APIS, "true");
        generator.setGeneratorPropertyDefault(CodegenConstants.SUPPORTING_FILES, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.API_TESTS, "false");
        generator.setGeneratorPropertyDefault(CodegenConstants.API_DOCS, "false");
    }

    @NotNull
    private static ClientOptInput createClientOptInput(OpenAPI openAPI, KotlinClientCodegen codegen) {
        ClientOptInput input = new ClientOptInput();
        input.openAPI(openAPI);
        input.config(codegen);
        return input;
    }

    private static OpenAPI readOpenAPI(String url) {
        return new OpenAPIParser()
                .readLocation(url, null, new ParseOptions()).getOpenAPI();
    }

    private KotlinClientCodegen createCodegen(ClientLibrary library) throws IOException {
        File output = Files.createTempDirectory("test").toFile().getCanonicalFile();
        output.deleteOnExit();
        KotlinClientCodegen codegen = new KotlinClientCodegen();
        codegen.setLibrary(library.getLibraryName());
        codegen.setOutputDir(output.getAbsolutePath());
        codegen.setSerializationLibrary(library.getSerializationLibrary());
        codegen.additionalProperties().put(CXFServerFeatures.LOAD_TEST_DATA_FROM_FILE, "true");
        codegen.additionalProperties().put(KotlinClientCodegen.USE_SPRING_BOOT3, "true");
        codegen.additionalProperties().put(KotlinClientCodegen.DATE_LIBRARY, "kotlinx-datetime");
        return codegen;
    }
}
