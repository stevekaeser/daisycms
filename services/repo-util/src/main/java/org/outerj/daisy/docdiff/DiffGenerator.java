/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.outerj.daisy.docdiff;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.xmlutil.XmlEncodingDetector;

import java.util.Arrays;

/**
 * This class generates information about the differences between two versions of a document.
 * The result of the comparision is generated as a series of events to an instance of
 * {@link DocDiffOutput}.
 */
public class DiffGenerator {
    private Version version1;
    private Version version2;
    private boolean hasRun = false;
    private DocDiffOutput output;
    private static final long DIFF_LIMIT = 500000; // about half a meg

    public static void generateDiff(Version version1, Version version2, DocDiffOutput output) throws Exception {
        new DiffGenerator(version1, version2, output).generateDiff();
    }

    private DiffGenerator(Version version1, Version version2, DocDiffOutput output) {
        this.version1 = version1;
        this.version2 = version2;
        this.output = output;
    }

    private void generateDiff() throws Exception {
        if (hasRun)
            throw new Exception("DiffGenerator object can only be used once");
        hasRun = true;

        output.begin();

        generatePartDifferences();
        generateFieldDifferences();
        generateLinkDifferences();

        output.end();

        version1 = null;
        version2 = null;
        output = null;
    }

    private void generatePartDifferences() throws Exception {
        output.beginPartChanges();

        Part[] version1Parts = version1.getPartsInOrder().getArray();
        Part[] version2Parts = version2.getPartsInOrder().getArray();

        // Search parts of version1 that are removed in version2
        for (Part version1Part : version1Parts) {
            long typeId = version1Part.getTypeId();
            if (findPart(version2Parts, typeId) == null) {
                output.partRemoved(version1Part);
            }
        }

        // Search for new or updated parts
        for (Part version2Part : version2Parts) {
            Part version1Part = findPart(version1Parts, version2Part.getTypeId());
            if (version1Part == null) {
                output.partAdded(version2Part);
            } else {
                if (version1Part.getSize() > DIFF_LIMIT || version2Part.getSize() > DIFF_LIMIT) {
                    output.partMightBeUpdated(version2Part);
                } else {
                    String version1MimeType = version1Part.getMimeType();
                    String version2MimeType = version2Part.getMimeType();

                    String version1FileName = version1Part.getFileName();
                    String version2FileName = version2Part.getFileName();

                    boolean mimeTypesEqual = version1MimeType.equals(version2MimeType);
                    boolean fileNamesEqual = (version1FileName == null && version2FileName == null) || (version1FileName != null && version1FileName.equals(version2FileName));
                    boolean dataEqual;

                    byte[] version1Data = null;
                    byte[] version2Data = null;

                    if (version1Part.getSize() != version2Part.getSize()) {
                        dataEqual = false;
                    } else {
                        version1Data = version1Part.getData();
                        version2Data = version2Part.getData();
                        dataEqual = Arrays.equals(version1Data, version2Data);
                    }

                    if (mimeTypesEqual && fileNamesEqual && dataEqual) {
                        // report there are no changes
                        output.partUnchanged(version2Part);
                    } else {
                        String part1String = null, part2String = null;
                        if (!dataEqual && version1MimeType.startsWith("text/") && version2MimeType.startsWith("text/")) {
                            // load data if it not already happened
                            if (version1Data == null) {
                                version1Data = version1Part.getData();
                                version2Data = version2Part.getData();
                            }

                            // TODO do some effort to detect the encoding of non-xml text formats, for example using
                            // the library found at http://jchardet.sourceforge.net/
                            if (version1MimeType.equals("text/xml"))
                                part1String = new String(version1Data, XmlEncodingDetector.detectEncoding(version1Data));
                            else
                                part1String = new String(version1Data);

                            if (version1MimeType.equals("text/xml"))
                                part2String = new String(version2Data, XmlEncodingDetector.detectEncoding(version2Data));
                            else
                                part2String = new String(version2Data);
                        }

                        output.partUpdated(version1Part, version2Part, part1String, part2String);

                        version1Data = null;
                        version2Data = null;
                    }
                }
            }
        }

        output.endPartChanges();
    }

    private void generateFieldDifferences() throws Exception {
        output.beginFieldChanges();

        Field[] version1Fields = version1.getFields().getArray();
        Field[] version2Fields = version2.getFields().getArray();

        // Search fields of version1 that are removed in version2
        for (Field version1Field : version1Fields) {
            long typeId = version1Field.getTypeId();
            if (findField(version2Fields, typeId) == null) {
                output.fieldRemoved(version1Field);
            }
        }

        // Search for new or updated fields
        for (Field version2Field : version2Fields) {
            Field version1Field = findField(version1Fields, version2Field.getTypeId());
            if (version1Field == null) {
                output.fieldAdded(version2Field);
            } else {
                boolean updated = (version1Field.isMultiValue() && !Arrays.equals((Object[])version1Field.getValue(), (Object[])version2Field.getValue()))
                        || (!version1Field.isMultiValue() && !version1Field.getValue().equals(version2Field.getValue()));
                if (updated) {
                    output.fieldUpdated(version1Field, version2Field);
                } else {
                    // unchanged: don't report
                }
            }
        }

        output.endFieldChanges();
    }

    private void generateLinkDifferences() throws Exception {
        output.beginLinkChanges();

        Link[] version1Links = version1.getLinks().getArray();
        Link[] version2Links = version2.getLinks().getArray();

        // Search links of version1 that are removed in version2
        for (Link version1Link : version1Links) {
            if (findLink(version2Links, version1Link) == null) {
                output.linkRemoved(version1Link);
            }
        }

        // Search for new or updated links
        for (Link version2Link : version2Links) {
            Link version1Link = findLink(version1Links, version2Link);
            if (version1Link == null) {
                output.linkAdded(version2Link);
            } else {
                // unchanged link: don't report
            }
        }

        output.endLinkChanges();
    }

    private Part findPart(Part[] parts, long typeId) {
        for (Part part : parts) {
            if (part.getTypeId() == typeId) {
                return part;
            }
        }
        return null;
    }

    private Field findField(Field[] fields, long typeId) {
        for (Field field : fields) {
            if (field.getTypeId() == typeId) {
                return field;
            }
        }
        return null;
    }

    private Link findLink(Link[] links, Link wantedLink) {
        for (Link link : links) {
            if (link.getTitle().equals(wantedLink.getTitle()) && link.getTarget().equals(wantedLink.getTarget())) {
                return link;
            }
        }
        return null;
    }

}
