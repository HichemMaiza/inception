/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.io.xml.dkprocore;

import static java.util.Arrays.asList;
import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.METHOD;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;
import static org.apache.commons.lang3.ArrayUtils.indexOf;
import static org.apache.commons.lang3.ArrayUtils.subarray;
import static org.apache.uima.fit.util.FSCollectionFactory.createFSArray;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.xml.type.XmlAttribute;
import org.dkpro.core.api.xml.type.XmlElement;
import org.dkpro.core.api.xml.type.XmlNode;
import org.dkpro.core.api.xml.type.XmlTextNode;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

public class XmlNodeUtils
{
    public static boolean hasAttributeWithValue(XmlElement e, String aAttribute, String aValue)
    {
        return getAttributeValue(e, aAttribute).map(v -> aValue.equals(v)).orElse(false);
    }

    static String serializeCasToXmlString(JCas aJCas) throws IOException
    {
        try (var out = new StringWriter()) {
            SAXTransformerFactory tf = XmlParserUtils.newTransformerFactory();
            TransformerHandler th = tf.newTransformerHandler();
            th.getTransformer().setOutputProperty(OMIT_XML_DECLARATION, "yes");
            th.getTransformer().setOutputProperty(METHOD, "xml");
            th.getTransformer().setOutputProperty(INDENT, "no");
            th.setResult(new StreamResult(out));

            Cas2SaxEvents serializer = new Cas2SaxEvents(th);
            serializer.process(aJCas);

            return out.toString();
        }
        catch (SAXException | TransformerConfigurationException e) {
            throw new IOException(e);
        }
    }

    static void parseXmlStringToCas(JCas aJCas, String aXml) throws IOException
    {
        try (var reader = new StringReader(aXml)) {
            parseXmlToCas(aJCas, new InputSource(reader));
        }
    }

    static void parseXmlToCas(JCas aJCas, InputSource aSource) throws IOException
    {
        CasXmlHandler handler = new CasXmlHandler(aJCas);

        try {
            var parser = XmlParserUtils.newSaxParser();
            parser.parse(aSource, handler);
        }
        catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    public static boolean hasAttributeValue(XmlElement aElement, String aAttribute, String aValue)
    {
        return getAttributeValue(aElement, aAttribute) //
                .map(value -> Objects.equals(value, aValue)) //
                .orElse(false);
    }

    public static Optional<String> getAttributeValue(XmlElement aElement, String aAttribute)
    {
        var attributes = aElement.getAttributes();

        if (attributes == null) {
            return Optional.empty();
        }

        return attributes.select(XmlAttribute.class) //
                .filter(a -> aAttribute.equals(a.getQName())) //
                .findFirst() //
                .map(XmlAttribute::getValue);
    }

    public static String getMandatoryAttributeValue(XmlElement aElement, String aAttribute)
    {
        var attributes = aElement.getAttributes();

        if (attributes == null) {
            throw new IllegalArgumentException("Element has no attribute [" + aAttribute + "]");
        }

        return attributes.select(XmlAttribute.class) //
                .filter(a -> aAttribute.equals(a.getQName())) //
                .findFirst() //
                .map(XmlAttribute::getValue) //
                .orElseThrow(() -> new IllegalArgumentException(
                        "Element has no attribute [" + aAttribute + "]"));
    }

    public static String textContent(XmlElement aElement)
    {
        var buf = new StringBuilder();
        dfs(aElement, n -> {
            if (n instanceof XmlTextNode) {
                var tn = (XmlTextNode) n;
                if (tn.getCaptured()) {
                    buf.append(tn.getCoveredText());
                }
                else {
                    buf.append(tn.getText());
                }
            }
        });
        return buf.toString();
    }

    public static List<XmlNode> dfs(XmlNode aNode)
    {
        var nodes = new ArrayList<XmlNode>();
        dfs(aNode, nodes::add);
        return nodes;
    }

    private static void dfs(XmlNode aNode, Consumer<XmlNode> aVisitor)
    {
        if (aNode instanceof XmlElement) {
            var children = ((XmlElement) aNode).getChildren();
            if (children != null) {
                children.forEach($ -> dfs($, aVisitor));
            }
        }
        aVisitor.accept(aNode);
    }

    /**
     * Removes the given element and all descendants except captures text nodes. Captured text nodes
     * must be preserved, otherwise it may break rendering or serialization because the text of the
     * captured text nodes is considered part of the document text.
     * 
     * @param aElement
     *            the element to remove.
     */
    public static void removeWithDescendantsFromTree(XmlElement aElement)
    {
        ArrayList<XmlNode> newSiblings = new ArrayList<>();
        var parent = aElement.getParent();
        var siblings = parent.getChildren().toArray(new XmlNode[parent.getChildren().size()]);
        var i = indexOf(siblings, aElement);
        asList(subarray(siblings, 0, i)).forEach(newSiblings::add);
        for (var node : dfs(aElement)) {
            if (node instanceof XmlElement) {
                removeElement((XmlElement) node);
            }
            if (node instanceof XmlTextNode && ((XmlTextNode) node).getCaptured()) {
                newSiblings.add(node);
            }
            else {
                node.removeFromIndexes();
            }
        }
        asList(subarray(siblings, i + 1, siblings.length)).forEach(newSiblings::add);
        parent.setChildren(createFSArray(aElement.getJCas(), newSiblings));
    }

    public static void removeFromTree(XmlElement... aElement)
    {
        for (var e : aElement) {
            ArrayList<XmlNode> newSiblings = new ArrayList<>();
            var parent = e.getParent();
            var siblings = parent.getChildren().toArray(new XmlNode[parent.getChildren().size()]);
            var i = indexOf(siblings, e);
            asList(subarray(siblings, 0, i)).forEach(newSiblings::add);
            var children = e.getChildren();
            if (children != null) {
                children.forEach(child -> hoistNode(newSiblings, parent, child));
            }
            asList(subarray(siblings, i + 1, siblings.length)).forEach(newSiblings::add);
            parent.setChildren(createFSArray(e.getJCas(), newSiblings));
            removeElement(e);
        }
    }

    private static void removeElement(XmlElement e)
    {
        var attributes = e.getAttributes();
        if (attributes != null) {
            for (var attribute : attributes) {
                attribute.removeFromIndexes();
            }
            attributes.removeFromIndexes();
        }
        e.removeFromIndexes();
    }

    private static void hoistNode(ArrayList<XmlNode> newSiblings, XmlElement parent, XmlNode child)
    {
        child.setParent(parent);
        newSiblings.add(child);
    }

    public static List<XmlElement> selectElements(XmlElement aElement, List<String> aPath)
    {
        if (aPath.size() == 0 || !aElement.getQName().equals(aPath.get(0))) {
            return Collections.emptyList();
        }

        if (aPath.size() == 1) {
            return asList(aElement);
        }

        var result = new ArrayList<XmlElement>();
        for (XmlElement child : aElement.getChildren().select(XmlElement.class)) {
            result.addAll(selectElements(child, aPath.subList(1, aPath.size())));
        }
        return result;
    }

}
