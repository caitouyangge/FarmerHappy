package service.crawler;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class CsvSplitterServiceTest {

    @Test
    void splitCsvFile_shouldWriteUtf8BomForExcelCompatibility_andHandleBomInHeader() throws Exception {
        String projectRoot = System.getProperty("user.dir");
        Path resultDir = Paths.get(projectRoot, "result");
        Path splitDir = Paths.get(projectRoot, "result", "split");
        Files.createDirectories(resultDir);
        Files.createDirectories(splitDir);

        // 清理旧文件（避免测试互相干扰）
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(splitDir, "*.csv")) {
            for (Path p : stream) {
                Files.deleteIfExists(p);
            }
        }

        // 生成一个带 BOM 的表头（模拟 python utf-8-sig 导出）
        String inputFileName = "_test_input.csv";
        Path input = resultDir.resolve(inputFileName);
        String content =
                "\uFEFF品名,规格,平均价,发布日期\n" +
                "测试品种,大,3.5,2025-12-18 00:00:00\n" +
                "测试品种,,3.6,2025-12-17\n";
        Files.write(input, content.getBytes(StandardCharsets.UTF_8));

        new CsvSplitterService().splitCsvFile(inputFileName);

        // 找到生成的 split 文件
        Path out = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(splitDir, "测试品种_*.csv")) {
            for (Path p : stream) {
                out = p;
                break;
            }
        }

        assertThat(out).as("应生成测试品种的拆分文件").isNotNull();

        byte[] bytes = Files.readAllBytes(out);
        assertThat(bytes.length).isGreaterThanOrEqualTo(3);
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);

        // 清理测试文件
        Files.deleteIfExists(input);
        Files.deleteIfExists(out);
    }
}


