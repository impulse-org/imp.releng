package org.eclipse.imp.releng.metadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.imp.releng.ReleaseEngineeringPlugin;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class UpdateSiteInfo {
    public static class CategoryDef {
        private final String fName;
        private final String fLabel;
        public CategoryDef(String name, String label) {
            fName= name;
            fLabel= label;
        }
        public String getLabel() {
            return fLabel;
        }
        public String getName() {
            return fName;
        }
        @Override
        public String toString() {
            return "<category " + getName() + ">";
        }
    }

    public static class FeatureRef {
        private final String fID;
        private final String fVersion;
        private final String fURL;
        private final String fCategory;
        public FeatureRef(String id, String vers, String url, String cat) {
            fID= id;
            fVersion= vers;
            fURL= url;
            fCategory= cat;
        }
        public String getCategory() {
            return fCategory;
        }
        public String getID() {
            return fID;
        }
        public String getURL() {
            return fURL;
        }
        public String getVersion() {
            return fVersion;
        }
        @Override
        public String toString() {
            return "<feature " + getID() + " " + getVersion() + ">";
        }
    }

    public final IProject fProject;

    public final IFile fManifestFile;

    /**
     * The XML Document for the update site manifest.
     */
    public Document fManifestDoc;

    /**
     * The set of contained features, represented by the XML
     * document nodes for the corresponding "feature" elements.
     */
    public Set<Node> fFeatureNodes= new HashSet<Node>();

    public List<FeatureRef> fFeatureRefs= new ArrayList<FeatureRef>();

    public String fURL;

    public String fDescription;

    public List<CategoryDef> fCategories= new ArrayList<CategoryDef>();

    public UpdateSiteInfo(IProject project, IFile manifestFile) throws ParserConfigurationException, SAXException, IOException {
        fProject= project;
        fManifestFile= manifestFile;

        ReleaseEngineeringPlugin.getMsgStream().println("Got manifest document for " + manifestFile.getLocation().toPortableString());

        DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder= factory.newDocumentBuilder();

        fManifestDoc= docBuilder.parse(new File(manifestFile.getLocation().toOSString()));

        Node siteNode= fManifestDoc.getChildNodes().item(0);

        for(int i= 0; i < siteNode.getChildNodes().getLength(); i++) {
            Node child= siteNode.getChildNodes().item(i);
            String childName= child.getNodeName();

            if (childName.equals("description")) {
                fURL= child.getAttributes().getNamedItem("url").getNodeValue();
                fDescription= child.getFirstChild().getNodeValue();
            } else if (childName.equals("category-def")) {
                String catName= child.getAttributes().getNamedItem("name").getNodeValue();
                String catLabel= child.getAttributes().getNamedItem("label").getNodeValue();
                CategoryDef catDef= new CategoryDef(catName, catLabel);

                fCategories.add(catDef);
            } else if (childName.equals("feature")) {
                String featID= child.getAttributes().getNamedItem("id").getNodeValue();
                String featVers= child.getAttributes().getNamedItem("version").getNodeValue();
                String featURL= child.getAttributes().getNamedItem("url").getNodeValue();
                String featCat= "???";
                NodeList featChildren= child.getChildNodes();

                for(int j=0; j < featChildren.getLength(); j++) {
                    Node featChild= featChildren.item(j);
                    if (featChild.getNodeName().equals("category")) {
                        // Assume the feature contains only 1 category element
                        featCat= featChild.getAttributes().getNamedItem("name").getNodeValue();
//                        if (findCategory(featCat) == null) {
//                            VersionIncrementerPlugin.getMsgStream().println("Warning: no such category: " + featCat);
//                        }
                    }
                }
                fFeatureRefs.add(new FeatureRef(featID, featVers, featURL, featCat));
            }
        }
    }

    public String findCategoryForFeature(FeatureInfo featInfo) {
        for(FeatureRef fr: fFeatureRefs) {
            if (fr.getID().equals(featInfo.fFeatureID)) {
                return fr.getCategory();
            }
        }
        return fCategories.get(0).fName;
    }

    public boolean addFeatureIfMissing(FeatureInfo feature) {
        // Don't add this feature version if it's already on the update site
        for(FeatureRef featRef: fFeatureRefs) {
            if (featRef.getID().equals(feature.fFeatureID)) {
                if (featRef.getVersion().equals(feature.fFeatureVersion)) {
                    return false; // already in the update site; no need to add it
                }
            }
        }

        String newURL= "features/" + feature.fFeatureID + "_" + feature.fFeatureVersion + ".jar";

        fFeatureRefs.add(new FeatureRef(feature.fFeatureID, feature.fFeatureVersion, newURL, findCategoryForFeature(feature)));
        return true;
    }

    public void rewriteManifest(List<Change> changes, List<IFile> changedFiles) {
        StringBuilder sb= new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); nl(sb);
        sb.append("<site>"); nl(sb);
        if (fURL != null) {
            sb.append("  <description url=\"" + fURL + "\">"); nl(sb);
            if (fDescription != null) {
                sb.append("    " + fDescription.trim()); nl(sb);
            }
            sb.append("  </description>"); nl(sb); nl(sb);
        }
        for(CategoryDef cat: fCategories) {
            sb.append("  <category-def name=\"" + cat.getName() + "\" label=\"" + cat.getLabel() + "\"/>"); nl(sb);
        }
        nl(sb);
        for(FeatureRef featRef: fFeatureRefs) {
            sb.append("  <feature url=\"" + featRef.getURL() + "\" id=\"" + featRef.getID() + "\" version=\"" + featRef.getVersion() + "\">"); nl(sb);
            sb.append("    <category name=\"" + featRef.getCategory() + "\"/>"); nl(sb);
            sb.append("  </feature>"); nl(sb);
            nl(sb);
        }
        sb.append("</site>"); nl(sb);

        TextFileChange tfc= new TextFileChange("Update site update for " + fManifestFile.getFullPath().toPortableString(), fManifestFile);
        long curFileLength= new File(fManifestFile.getLocation().toOSString()).length();

        tfc.setEdit(new MultiTextEdit());
        tfc.addEdit(new ReplaceEdit(0, (int) curFileLength, sb.toString()));

        changedFiles.add(fManifestFile);
        changes.add(tfc);
    }

    protected void nl(StringBuilder sb) {
        sb.append("\n");
    }

    @Override
    public String toString() {
        return "<update site " + fManifestFile.getFullPath().toPortableString() + ">";
    }
}
