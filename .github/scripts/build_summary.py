#!/usr/bin/env python3
"""빌드 결과(커버리지, 정적 분석)를 마크다운 표로 요약한다. CI의 PR 코멘트에 쓴다."""
import glob
import xml.etree.ElementTree as ET


def module_of(path: str) -> str:
    return path.split("/build/")[0] or "."


def coverage_rows():
    rows = []
    for path in sorted(glob.glob("**/build/reports/jacoco/test/jacocoTestReport.xml", recursive=True)):
        root = ET.parse(path).getroot()
        counters = {c.get("type"): c for c in root.findall("counter")}

        def ratio(kind):
            c = counters.get(kind)
            if c is None:
                return "-"
            missed, covered = int(c.get("missed")), int(c.get("covered"))
            total = missed + covered
            return f"{covered / total:.1%}" if total else "-"

        rows.append(f"| `{module_of(path)}` | {ratio('LINE')} | {ratio('BRANCH')} |")
    return rows


def count(pattern: str, tag: str) -> int:
    total = 0
    for path in glob.glob(pattern, recursive=True):
        total += len(ET.parse(path).getroot().findall(f".//{tag}"))
    return total


def main():
    print("## 빌드 결과")
    print()
    print("### 커버리지 (게이트: 라인·브랜치 80%)")
    rows = coverage_rows()
    if rows:
        print("| 모듈 | 라인 | 브랜치 |")
        print("| --- | --- | --- |")
        print("\n".join(rows))
    else:
        print("커버리지 리포트가 없습니다. 빌드가 테스트 전에 실패했을 수 있습니다.")
    print()
    print("### 정적 분석")
    print("| 도구 | 위반 |")
    print("| --- | --- |")
    print(f"| Checkstyle | {count('**/build/reports/checkstyle/*.xml', 'error')} |")
    print(f"| SpotBugs | {count('**/build/reports/spotbugs/*.xml', 'BugInstance')} |")


if __name__ == "__main__":
    main()
