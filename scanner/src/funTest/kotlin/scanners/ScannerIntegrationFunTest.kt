/*
 * Copyright (C) 2017 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.scanner.scanners

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import java.io.File

import org.ossreviewtoolkit.model.LicenseFinding
import org.ossreviewtoolkit.model.PackageType
import org.ossreviewtoolkit.model.ScanSummary
import org.ossreviewtoolkit.model.ScannerDetails
import org.ossreviewtoolkit.model.TextLocation
import org.ossreviewtoolkit.model.config.DownloaderConfiguration
import org.ossreviewtoolkit.model.config.ScannerConfiguration
import org.ossreviewtoolkit.model.readValue
import org.ossreviewtoolkit.scanner.PathScannerWrapper
import org.ossreviewtoolkit.scanner.ScanContext
import org.ossreviewtoolkit.scanner.Scanner
import org.ossreviewtoolkit.scanner.ScannerCriteria
import org.ossreviewtoolkit.scanner.provenance.DefaultNestedProvenanceResolver
import org.ossreviewtoolkit.scanner.provenance.DefaultPackageProvenanceResolver
import org.ossreviewtoolkit.scanner.provenance.DefaultProvenanceDownloader
import org.ossreviewtoolkit.scanner.provenance.DummyNestedProvenanceStorage
import org.ossreviewtoolkit.scanner.provenance.DummyProvenanceStorage
import org.ossreviewtoolkit.scanner.utils.DefaultWorkingTreeCache
import org.ossreviewtoolkit.utils.spdx.SpdxConstants
import org.ossreviewtoolkit.utils.test.getAssetFile
import org.ossreviewtoolkit.utils.test.patchActualResult
import org.ossreviewtoolkit.utils.test.patchExpectedResult
import org.ossreviewtoolkit.utils.test.toYaml

class ScannerIntegrationFunTest : StringSpec({
    "Gradle project scan results for a given analyzer result are correct".config(invocations = 3) {
        val analyzerResultFile = getAssetFile("analyzer-result.yml")
        val expectedResultFile = getAssetFile("dummy-expected-output-for-analyzer-result.yml")
        val expectedResult = patchExpectedResult(expectedResultFile)

        val downloaderConfiguration = DownloaderConfiguration()
        val workingTreeCache = DefaultWorkingTreeCache()
        val provenanceDownloader = DefaultProvenanceDownloader(downloaderConfiguration, workingTreeCache)
        val packageProvenanceStorage = DummyProvenanceStorage()
        val nestedProvenanceStorage = DummyNestedProvenanceStorage()
        val packageProvenanceResolver = DefaultPackageProvenanceResolver(packageProvenanceStorage, workingTreeCache)
        val nestedProvenanceResolver = DefaultNestedProvenanceResolver(nestedProvenanceStorage, workingTreeCache)
        val dummyScanner = DummyScanner()

        val scanner = Scanner(
            scannerConfig = ScannerConfiguration(),
            downloaderConfig = downloaderConfiguration,
            provenanceDownloader = provenanceDownloader,
            storageReaders = emptyList(),
            storageWriters = emptyList(),
            packageProvenanceResolver = packageProvenanceResolver,
            nestedProvenanceResolver = nestedProvenanceResolver,
            scannerWrappers = mapOf(
                PackageType.PROJECT to listOf(dummyScanner),
                PackageType.PACKAGE to listOf(dummyScanner)
            )
        )

        val result = scanner.scan(analyzerResultFile.readValue(), skipExcluded = false, emptyMap())

        patchActualResult(result.toYaml(), patchStartAndEndTime = true) shouldBe expectedResult
    }
})

private class DummyScanner : PathScannerWrapper {
    override val details = ScannerDetails(name = "Dummy", version = "1.0.0", configuration = "")
    override val criteria = ScannerCriteria.forDetails(details)

    override fun scanPath(path: File, context: ScanContext): ScanSummary {
        val licenseFindings = path.listFiles().orEmpty().mapTo(sortedSetOf()) { file ->
            LicenseFinding(
                license = SpdxConstants.NONE,
                location = TextLocation(file.relativeTo(path).invariantSeparatorsPath, TextLocation.UNKNOWN_LINE)
            )
        }

        return ScanSummary.EMPTY.copy(
            licenseFindings = licenseFindings
        )
    }
}
