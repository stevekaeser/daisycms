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
package org.outerj.daisy.htmlcleaner;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import junit.framework.TestCase;

import org.xml.sax.InputSource;

public class HtmlCleanerTest extends TestCase {
    public void testIt() throws Exception {
        HtmlCleanerFactory factory = new HtmlCleanerFactory();
        InputSource is = new InputSource(getClass().getClassLoader().getResourceAsStream("org/outerj/daisy/htmlcleaner/cleanerconf.xml"));
        HtmlCleanerTemplate template = factory.buildTemplate(is);

        String result;
        HtmlCleaner cleaner = template.newHtmlCleaner();
        // the \u0004 in there is to check that invalid XML characters are removed
        result = cleaner.cleanToString("<html><body>\u0004abc</body></html>");
        assertEquals(readResource("output1.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html xmlns='abc'><body>abc<ul> </ul></body></html>");
        assertEquals(readResource("output1.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<x:html xmlns:x='abc'><x:body x:r='z'>abc</x:body></x:html>");
        assertEquals(readResource("output1.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("abc");
        assertEquals(readResource("output1.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html>abc</html>");
        assertEquals(readResource("output1.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body>abc</html>");
        assertEquals(readResource("output1.txt"), result);

        // * free text in body should be embedded in <p>
        // * two br's should be translated to new paragraph
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body>abc<br/><br/>def</html>");
        assertEquals(readResource("output2.txt"), result);

        // * more then two br's should give same result
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body>abc<br/><br/><br/>def</html>");
        assertEquals(readResource("output2.txt"), result);

        // * two br's are translated to new paragraph
        // * one br remains one br
        // * one or more br's before </p> closing tag: remove them
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body>abc<br/><br/>def<p>xyz<br/>xyz</p><p>yes<br/></p><p>yesyes<br/><br/><br/></html>");
        assertEquals(readResource("output3.txt"), result);

        // * table inside a <p> tag
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><p><table><tr><td>hello!</td></tr></table></p></html>");
        assertEquals(readResource("output4.txt"), result);

        // ul inside a p tag
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><p><ul><li>hello!</li></ul></p></html>");
        assertEquals(readResource("output5.txt"), result);

        // ul inside a p tag with still some text around it
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><p>abc<ul><li>hello!</li></ul>def</p></html>");
        assertEquals(readResource("output6.txt"), result);

        // test text reflow
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><p>Hi, this is a text longer then 80 characters which will hence be split across multiple lines. Isn't this interesting. No it isn't. Anyhow, have I told you about that time when I invented the wheel? Well, it was a long time ago.</p></html>");
        assertEquals(readResource("output7.txt"), result);

        // test removal of not-allowed tags
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><p><font>abc</font></p></html>");
        assertEquals(readResource("output1.txt"), result);

        // test translation of span with styling
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><p><span style='color: green; font-weight:bold  '>abc</span><span style='font-style:italic'>abc</span><span style='font-style:italic;font-weight:bold'>abc</span></p></html>");
        assertEquals(readResource("output8.txt"), result);

        // test img src conversion
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><img src='hi' daisysrc='daisy:123'/></body></html>");
        assertEquals(readResource("output9.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body>Hi this is <strong>strong</strong> and <em>emphasized</em></body></html>");
        assertEquals(readResource("output10.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body>Hi this is <strong>strong</strong><em>emphasized</em></body></html>");
        assertEquals(readResource("output11.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body>Hi this is <strong>strong</strong> <em>emphasized</em></body></html>");
        assertEquals(readResource("output12.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body>aaaa bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb ccc</body></html>");
        assertEquals(readResource("output13.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body>aaaa bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb<img src='somewhere'/></body></html>");
        assertEquals(readResource("output14.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body>test test test test test test test                      \n\n test test test test test test test<a href='http://outerthought.org'>test</a> test</body></html>");
        assertEquals(readResource("output15.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><strong>x</strong><img src='x.gif'/><strong>x</strong><img src='x.gif'/><strong>x</strong><img src='x.gif'/><strong>x</strong><img src='x.gif'/><strong>x</strong><img src='x.gif'/></html>");
        assertEquals(readResource("output16.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><p> a b c </p></body></html>");
        assertEquals(readResource("output17.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><p> a b c </p>   </body></html>");
        assertEquals(readResource("output17.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body> a b c <br/> </html>");
        assertEquals(readResource("output17.txt"), result);

        // test removal of <br> inside <td>
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><table><tbody><tr><td><br/></td></tr></tbody></table></html>");
        assertEquals(readResource("output18.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><table><tbody><tr><td><br/>\n</td></tr></tbody></table></html>");
        assertEquals(readResource("output18.txt"), result);

        String teststring = "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"/daisy/resources/skins/default/css/htmlarea.css\" /></head>\n" +
                " <body>\n" +
                " \n" +
                " <p><strong>asfasdfa</strong></p>\n" +
                " \n" +
                " <p><strong>dfsafsa<br /></strong></p><p><strong><br />asfj aflad <span style=\"font-style: italic;\">fafjls fd<br /></span></strong></p><p><strong><span style=\"font-style: italic;\">saj lfsdj </span>lkjlkjweids<br /></strong></p>\n" +
                " \n" +
                " </body></html>";

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString(teststring);
        assertEquals(readResource("output19.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><p>abc<strong/></p><p><strong><em><em><em/></em></em></strong></p></body></html>");
        assertEquals(readResource("output1.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><table><tr><td> <br/></td></tr></table></body></html>");
        assertEquals(readResource("output20.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body>hallo<table><tr><td>nog eens hallo</td></tr></table></body></html>");
        assertEquals(readResource("output21.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><p>hallo<table><tr><td>nog eens hallo</td></tr></table></p></body></html>");
        assertEquals(readResource("output21.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><p>hallo<table><tr><td>nog eens hallo<br/><br/>jaja<p>jan piet joris</p></td><td><table><tr><td><p>1</p>2</td></tr></table></td></tr></table></p></body></html>");
        assertEquals(readResource("output22.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><pre>each<br/>word<br/>on a new<br/>line</pre></body></html>");
        assertEquals(readResource("output23.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><h1>ab<br/></h1><h1><br/>\n</h1><h1><br/><h2><br/>cd</h2>ef</h1></body></html>");
        assertEquals(readResource("output24.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body>klsaflkjdkadjfkajlfksdjakfdsfka&lt;abc&gt;lsjfladjflsafjlsjflkjaskfjlkjflksjafkdjalfsajfkjalfdlsfaj</body></html>");
        assertEquals(readResource("output25.txt"), result);

        // test link href conversion
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><a href='hi' daisyhref='daisy:123'>boe</a></body></html>");
        assertEquals(readResource("output26.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body>boe <div class='daisy-include-preview'>something<b>more</b></div> boe</body></html>");
        assertEquals(readResource("output27.txt"), result);

        // test wrapping stand-alone li's into an ul (sometimes happens in old html copied from the web)
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><li>first flying li<li>second flying li<li>third flying li</body></html>");
        assertEquals(readResource("output28.txt"), result);

        // check same thing with presence of nested ul
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><li>first flying li<li>second flying li<ul><li>foo<li>bar</ul><li>third flying li</body></html>");
        assertEquals(readResource("output29.txt"), result);

        // content of blockquote's should be wrapped in p's, since it doesn't allow inline child tags
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body> <blockquote>foobar</blockquote> </body></html>");
        assertEquals(readResource("output30.txt"), result);

        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><ul><li>foo</li><br/></ul></body></html>");
        assertEquals(readResource("output31.txt"), result);

        // a case where a new <ul> should be introduced, but outside of the <p>
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><p><li>foo</li></p></body></html>");
        assertEquals(readResource("output32.txt"), result);

        // test removal of <style> tag and its content
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><style>hello<b>hello</b></style><script>hello</script></body></html>");
        assertTrue(result.indexOf("hello") == -1);

        // * table attributes should not be removed
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><p><table width=\"100px\" print-width=\"200px\" removedfoo=\"bar\"><tr><td>hello!</td></tr></table></p></html>");
        assertEquals(readResource("output33.txt"), result);

        // * bug: NPE throwage
        cleaner = template.newHtmlCleaner();
        result = cleaner.cleanToString("<html><body><b>hello</b></body></html>");
        assertEquals(readResource("output34.txt"), result);

    }

    String readResource(String name) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("org/outerj/daisy/htmlcleaner/" + name);
        Reader reader = new InputStreamReader(is, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(reader);

        StringBuilder buffer = new StringBuilder();
        int c = bufferedReader.read();
        while (c != -1) {
            buffer.append((char)c);
            c = bufferedReader.read();
        }

        return buffer.toString();
    }
}
