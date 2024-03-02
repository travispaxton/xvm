package org.xvm.tool;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.xvm.util.ConstOrdinalList;

/**
 * Using the raw information from {@code ./resources/unicode/*.zip}, build the Unicode data tables
 * used by the Char class.
 */
public class BuildUnicodeTables {
    public static final boolean TEST = Boolean.parseBoolean(System.getenv().getOrDefault("UNICODE_TEST", null));

    private static final String TEST_RESOURCE_XML = "test.xml";

    private static final String UCD_ALL_FLAT_ZIP = "ucd.all.flat.zip";
    private static final String UCD_ALL_FLAT_XML = "ucd.all.flat.xml";
    private static final File OUTPUT_DIR = new File("./build/resources/unicode/");

    private static final int BUF_SIZE = 256;
    private static final long MB = 1024L * 1024L;
    private static final long GB = MB * 1024L;

    private final File destinationDir;
    private final File ucdZipFile;

    /**
     * Build unicode tables based on the downloaded ucd zip file.
     *
     * @param ucdZipFile mandatory path to ucd zip file (created by download task or taken from Gradle cache)
     */
    @SuppressWarnings("unused")
    public BuildUnicodeTables(final File ucdZipFile) {
        this(ucdZipFile, OUTPUT_DIR);
    }

    public BuildUnicodeTables(final File ucdZipFile, final File destinationDir) {
        this.ucdZipFile = requireNonNull(ucdZipFile);
        this.destinationDir = destinationDir;
    }

    /**
     * Entry point from the OS.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) throws IOException, JAXBException {
        new BuildUnicodeTables(args.length > 0 ? new File(args[0]) : null, args.length > 1 ? new File(args[1]) : null).run();
    }

    /**
     * Execute the Launcher tool.
     */
    public void run() throws IOException, JAXBException {
        out("Locating Unicode raw data ...");

        final var listRaw = loadData();
        final int nHigh = listRaw.stream().mapToInt(CharData::lastIndex).max().orElse(-1);
        final int cAll = nHigh + 1;

        out("Processing Unicode codepoints 0.." + nHigh);

        // Various data collections
        final int[] cats = new int[cAll];
        Arrays.fill(cats, new CharData().cat());

        final int[] decs = new int[cAll];
        Arrays.fill(decs, 10); // 10 is illegal; use as "null"

        final int[] cccs   = new int[cAll];
        final int[] lowers = new int[cAll];
        final int[] uppers = new int[cAll];
        final int[] titles = new int[cAll];

        Arrays.fill(cccs, 255); // 255 is illegal; use as "null"

        final String[] blocks = new String[cAll];
        final String[] nums   = new String[cAll];

        listRaw.forEach(cd -> {
            final int first = cd.firstIndex();
            final int last  = cd.lastIndex();
            for (int i = first; i <= last; i++) {
                cats[i]   = cd.cat();
                decs[i]   = cd.dec();
                nums[i]   = cd.num();
                cccs[i]   = cd.combo();
                lowers[i] = cd.lower();
                uppers[i] = cd.upper();
                titles[i] = cd.title();
                blocks[i] = cd.block();
            }
        });

        writeResult("Cats", cats);
        writeResult("Decs", decs);
        writeResult("Nums", nums);
        writeResult("CCCs", cccs);
        writeResult("Lowers", lowers);
        writeResult("Uppers", uppers);
        writeResult("Titles", titles);
        writeResult("Blocks", blocks);
    }

    public List<CharData> loadData() throws IOException, JAXBException {
        if (TEST) {
            return getRepertoire(loadDataTest());
        }

        final var zip = getZipFile();
        final ZipEntry entryXML = zip.getEntry(UCD_ALL_FLAT_XML);
        final long lRawLen = entryXML.getSize();
        assert 2 * GB > lRawLen;

        final int cbRaw = (int)lRawLen;
        final byte[] abRaw = new byte[cbRaw];
        try (var in = zip.getInputStream(entryXML)) {
            final int cbActual = in.readNBytes(abRaw, 0, cbRaw);
            assert cbActual == cbRaw;
            return getRepertoire(new String(abRaw));
        }
    }

    private static List<CharData> getRepertoire(final String xml) throws JAXBException {
        final var jaxbUnmarshaller = JAXBContext.newInstance(UCDData.class).createUnmarshaller();
        final var data = (UCDData)jaxbUnmarshaller.unmarshal(new StringReader(xml));
        return data.repertoire;
    }

    private static String loadDataTest() throws IOException {
        final var loader = requireNonNullElseGet(
                BuildUnicodeTables.class.getClassLoader(),
                ClassLoader::getSystemClassLoader);
        final var file = readableFile(requireNonNull(loader.getResource(TEST_RESOURCE_XML)));
        final long lRawLen = file.length();

        assert 2 * GB > lRawLen;

        final int cbRaw = (int)lRawLen;
        final byte[] abRaw = new byte[cbRaw];
        try (var in = new FileInputStream(file)) {
            final int cbActual = in.readNBytes(abRaw, 0, cbRaw);
            assert cbActual == cbRaw;
        }

        return new String(abRaw);
    }

    private static File readableFile(final URL url) {
        return checkReadable(new File(url.getFile()));
    }

    private static File checkReadable(final File file) {
        assert isReadable(file) : file;
        return file;
    }

    private static boolean isReadable(final File file) {
        return file.isFile() && file.canRead();
    }

    private ZipFile getZipFile() throws IOException {
        final var file = requireNonNullElseGet(ucdZipFile, () -> new File(UCD_ALL_FLAT_ZIP));

        if (!isReadable(file)) {
            final var loader   = requireNonNullElseGet(BuildUnicodeTables.class.getClassLoader(), ClassLoader::getSystemClassLoader);
            final var resource = loader.getResource(UCD_ALL_FLAT_ZIP);
            if (resource == null) {
                throw new IOException("Cannot find resources for unicode file: " + UCD_ALL_FLAT_ZIP);
            }
            return new ZipFile(resource.getFile());
        }

        out("Reverting to zip file: " + file.getAbsolutePath());
        return new ZipFile(file);
    }

    private void writeResult(final String name, final String[] array) throws IOException {
        // Collect and sort the values
        final var map = new TreeMap<String, Integer>();
        // TODO use Collectors.toMap here instead of hte hard to read compute lambda
        Arrays.stream(array).filter(BuildUnicodeTables::isNotEmpty).forEach(s -> map.compute(s, (k, v) -> (v == null ? 0 : v) + 1));
        for (final var s : array) {
            if (s != null) {
                assert !s.isEmpty();
                map.compute(s, (k, v) -> (v == null ? 0 : v) + 1);
            }
        }

        final var sb = new StringBuilder(name);
        sb.append(": [index] \"str\" (freq) \n--------------------");

        int index = 0;
        for (final var entry : map.entrySet()) {
            sb.append("\n[")
                    .append(index)
                    .append("] \"")
                    .append(entry.getKey())
                    .append("\" (")
                    .append(entry.getValue()).append("x)");
            entry.setValue(index++);
        }

        final int indexNull = index;
        sb.append("\n\ndefault=")
                .append(indexNull);

        writeDetails(name, sb.toString());

        // assign indexes to each
        final int[] an = new int[array.length];
        for (int i = 0; i < an.length; i++) {
            final String s = array[i];
            an[i] = null == s ? indexNull : map.get(s);
        }
        writeResult(name, an);
    }

    private void writeResult(final String name, final int[] array) throws IOException {
        writeResult(name, ConstOrdinalList.compress(array, BUF_SIZE));
    }

    private File resolveOutput(final String name, final String extension) throws IOException {
        final var filename = "Char" + name + '.' + extension;
        final var dir = destinationDir;
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not access or create dir: '" + dir.getAbsolutePath() + '\'');
        }
        assert dir.canWrite();
        return new File(dir, filename);
    }

    private void writeResult(final String name, final byte[] data) throws IOException {
        out("writeResult " + name);
        try (var out = new FileOutputStream(resolveOutput(name, "dat"))) {
            out.write(data);
        }
    }

    private void writeDetails(final String name, final String details) throws IOException {
        out("writeResult " + name);
        try (var out = new FileWriter(resolveOutput(name, "txt"))) {
            out.write(details);
        }
    }

    private static boolean isNullOrEmpty(final String str) {
        return str == null || str.isEmpty();
    }

    private static boolean isNotEmpty(final String str) {
        return !isNullOrEmpty(str);
    }

    /**
     * Log a blank line
     */
    public static void out() {
        out("");
    }

    /**
     * Log the String value of some object to the terminal.
     */
    public static void out(final Object o) {
        System.out.println(BuildUnicodeTables.class.getSimpleName() + ": " + o);
    }

    /**
     * Print a blank line to the terminal.
     */
    @SuppressWarnings("unused")
    public static void err() {
        err("");
    }

    /**
     * Print the String value of some object to the terminal.
     */
    public static void err(final Object o) {
        System.err.println(BuildUnicodeTables.class.getSimpleName() + ": " + o);
    }

    /**
     * UCDData is a simple data class for the JAXB parser.
     */
    @XmlRootElement(name = "ucd")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class UCDData {
        @XmlElement
        public String description;

        @XmlElements({
                @XmlElement(name = "char"),
                @XmlElement(name = "noncharacter"),
                @XmlElement(name = "surrogate"),
                @XmlElement(name = "reserved")})
        @XmlElementWrapper
        public List<CharData> repertoire = new ArrayList<>();

        @Override
        public String toString() {
            final var sb = new StringBuilder("UCD description=")
                    .append(description)
                    .append(", repertoire=\n");
            int c = 0;
            for (final var item : repertoire) {
                if (c > BUF_SIZE) {
                    sb.append(",\n...");
                    break;
                } else if (c++ > 0) {
                    sb.append(",\n");
                }
                sb.append(item);
            }
            return sb.toString();
        }
    }

    /**
     * CharData is a simple data class for the JAXB parser.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CharData {
        @XmlAttribute(name = "cp")
        String codepoint;

        @XmlAttribute(name = "first-cp")
        String codepointStart;

        @XmlAttribute(name = "last-cp")
        String codepointEnd;

        @XmlAttribute(name = "na")
        String name;

        @XmlAttribute(name = "gc")
        String gc;

        @XmlAttribute(name = "nt")
        String nt;

        @XmlAttribute(name = "nv")
        String nv;

        @XmlAttribute(name = "ccc")
        String ccc;

        @XmlAttribute(name = "slc")
        String slc;

        @XmlAttribute(name = "suc")
        String suc;

        @XmlAttribute(name = "stc")
        String stc;

        @XmlAttribute(name = "blk")
        String blk;

        @SuppressWarnings("checkstyle:MagicNumber")
        int cat() {
            if (null == gc) {
                return 29;
            }

            return switch (gc) {
            case "Lu" -> 0;
            case "Ll" -> 1;
            case "Lt" -> 2;
            case "Lm" -> 3;
            case "Lo" -> 4;
            case "Mn" -> 5;
            case "Mc" -> 6;
            case "Me" -> 7;
            case "Nd" -> 8;
            case "Nl" -> 9;
            case "No" -> 10;
            case "Pc" -> 11;
            case "Pd" -> 12;
            case "Ps" -> 13;
            case "Pe" -> 14;
            case "Pi" -> 15;
            case "Pf" -> 16;
            case "Po" -> 17;
            case "Sm" -> 18;
            case "Sc" -> 19;
            case "Sk" -> 20;
            case "So" -> 21;
            case "Zs" -> 22;
            case "Zl" -> 23;
            case "Zp" -> 24;
            case "Cc" -> 25;
            case "Cf" -> 26;
            case "Cs" -> 27;
            case "Co" -> 28;
            default -> 29;
            };
        }

        int firstIndex() {
            return isNullOrEmpty(codepoint) ? Integer.parseInt(codepointStart, 16) : Integer.parseInt(codepoint, 16);
        }

        int lastIndex() {
            return isNullOrEmpty(codepoint) ? Integer.parseInt(codepointEnd, 16) : Integer.parseInt(codepoint, 16);
        }

        int dec() {
            if ("De".equals(nt)) {
                assert null != nv;
                assert !nv.isEmpty();
                assert !"NaN".equals(nv);
                return Integer.parseInt(nv);
            }

            return 10; // illegal value
        }

        String num() {
            return isNullOrEmpty(nt) || "None".equals(nt) || isNullOrEmpty(nv) || "NaN".equals(nv) ? null : nv;
        }

        int combo() {
            return isNullOrEmpty(ccc) ? 255 : Integer.parseInt(ccc);
        }

        int lower() {
            return isNullOrEmpty(slc) || "#".equals(slc) ? 0 : Integer.parseInt(slc, 16);
        }

        int upper() {
            return isNullOrEmpty(suc) || "#".equals(suc) ? 0 : Integer.parseInt(suc, 16);
        }

        int title() {
            return isNullOrEmpty(stc) || "#".equals(stc) ? 0 : Integer.parseInt(stc, 16);
        }

        String block() {
            return isNullOrEmpty(blk) ? null : blk;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName().toLowerCase()
                + " codepoint="
                + (codepoint == null || codepoint.isEmpty()
                    ? codepointStart + ".." + codepointEnd
                    : codepoint)
                + (name != null && !name.isEmpty()
                    ? ", name=\"" + name + "\"" : "") + ", gen-cat=" + gc + (null != blk && !blk.isEmpty()
                    ? ", block=\"" + blk + "\"" : "") + (nt != null && !nt.isEmpty() && !"None".equals(nt)
                    ? ", num-type=\"" + nt + "\"" : "") + (nv != null && !nv.isEmpty() && !"NaN".equals(nv)
                    ? ", num-val=\"" + nv + "\"" : "") + (suc == null || suc.isEmpty() || "#".equals(suc)
                    ? "" : ", suc=" + suc)
                + (slc == null || slc.isEmpty() || "#".equals(slc) ? "" : ", slc=" + slc)
                + (null == stc || stc.isEmpty() || "#".equals(stc) ? "" : ", stc=" + stc);
        }
    }
}
