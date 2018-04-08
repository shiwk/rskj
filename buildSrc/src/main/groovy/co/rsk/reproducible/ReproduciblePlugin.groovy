package co.rsk.reproducible

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ReproduciblePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.ext.gitCurrentBranch = this.&gitCurrentBranch
        project.ext.gitCommitHash = this.&gitCommitHash

        project.task("reproducible", dependsOn: 'shadowJar') {
            doLast {
                long fixDateJar = gitDateHash() * 1000
                File newJar = new File(project.shadowJar.archivePath.parent, 'tmp-' + project.shadowJar.archiveName)
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(newJar))
                JarFile jf = new JarFile(project.shadowJar.archivePath)
                jf.entries().each { entry ->
                    cloneAndCopyEntry(jf, entry, zos, fixDateJar)
                }
                zos.finish()
                jf.close()
                // -- Enable this line to debug -- //
                //compareJars(archivePath, newJar, fixDateJar)
                // -- Remove old Jar -- //
                Files.deleteIfExists(Paths.get(project.shadowJar.archivePath.getAbsolutePath()));
                // -- Rename reproducible Jar -- //
                newJar.renameTo(project.shadowJar.archivePath)
            }
        }
    }

    static String gitCurrentBranch() {
        def process = "git rev-parse --abbrev-ref HEAD".execute()
        return process.text
    }

    static String gitCommitHash() {
        def process = "git rev-parse --short HEAD".execute()
        return process.text
    }

    static Long gitDateHash() {
        def process = "git show -s --format=%ct ${gitCommitHash()}".execute()
        return process.text.toLong()
    }

    static void compareJars(File original, File copy, long ts) {
        def jf = new JarFile(original)
        def cjf = new JarFile(copy)
        jf.entries().each { entry ->
            def centry = cjf.getJarEntry(entry.name)
            compareEntries(entry, centry, ts)
        }
    }

    static void compareEntries(JarEntry entry, JarEntry centry, long ts) {
        assert entry.name == centry.name
        assert entry.comment == centry.comment
        assert entry.compressedSize == centry.compressedSize
        assert entry.crc == centry.crc
        assert entry.extra == centry.extra
        assert entry.method == centry.method
        assert entry.size == centry.size
        assert ts == centry.time
        assert entry.hashCode() == centry.hashCode()
    }

    static void cloneAndCopyEntry(JarFile originalFile, JarEntry original, ZipOutputStream zos, long newTimestamp) {
        ZipEntry clone = new ZipEntry(original)
        clone.time = newTimestamp
        def entryIs = originalFile.getInputStream(original)
        zos.putNextEntry(clone)
        copyBinaryData(entryIs, zos)
    }

    static void copyBinaryData(InputStream is, ZipOutputStream zos) {
        byte[] buffer = new byte[1024*1024]
        int len = 0
        while((len = is.read(buffer)) != -1) {
            zos.write(buffer, 0, len)
        }
    }
}

