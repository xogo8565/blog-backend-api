package com.example.blog.kernel.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** ErrorCode enum과 docs/api/openapi.yaml의 ErrorCode 목록이 같은지 검사한다. */
class ErrorCodeSpecTest {

  private static final Path SPEC = Path.of("../../docs/api/openapi.yaml");
  private static final Pattern ENUM_ITEM = Pattern.compile("^\\s+- ([A-Z_]+)\\s*$");
  private static final Pattern NEXT_SCHEMA = Pattern.compile("^ {4}\\S.*$");

  @Test
  @DisplayName("ErrorCode는 openapi.yaml의 ErrorCode와 순서까지 같다")
  void matchesOpenApiSpec() throws IOException {
    List<String> specCodes = readSpecCodes(Files.readAllLines(SPEC));
    List<String> enumCodes = Arrays.stream(ErrorCode.values()).map(Enum::name).toList();

    assertThat(specCodes).isNotEmpty();
    assertThat(enumCodes).containsExactlyElementsOf(specCodes);
  }

  private static List<String> readSpecCodes(List<String> lines) {
    List<String> codes = new ArrayList<>();
    boolean inErrorCode = false;
    for (String line : lines) {
      if (line.equals("    ErrorCode:")) {
        inErrorCode = true;
        continue;
      }
      if (!inErrorCode) {
        continue;
      }
      if (NEXT_SCHEMA.matcher(line).matches()) {
        break;
      }
      Matcher matcher = ENUM_ITEM.matcher(line);
      if (matcher.matches()) {
        codes.add(matcher.group(1));
      }
    }
    return codes;
  }
}
