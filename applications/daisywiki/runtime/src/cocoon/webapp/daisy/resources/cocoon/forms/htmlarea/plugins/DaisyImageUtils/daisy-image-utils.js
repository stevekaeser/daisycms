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

function DaisyImageUtils(editor, params) {
	this.editor = editor;
	var cfg = editor.config;
	var toolbar = cfg.toolbar;
	var self = this;
	var i18n = DaisyImageUtils.I18N;
	var plugin_config = params[0];

    cfg.registerButton("daisy-insert-image", i18n["hint.insert-new-image"], editor.imgURL("insert-image.gif", "DaisyImageUtils"), false,
               function(editor, id) {
                   self.insertImage(editor, id);
               }, null);
    cfg.registerButton("daisy-alter-image", i18n["hint.change-image-properties"], editor.imgURL("alter-image.gif", "DaisyImageUtils"), false,
               function(editor, id) {
                   self.alterImage(editor, id);
               }, null);

};

DaisyImageUtils._pluginInfo = {
	name          : "DaisyImageUtils",
	version       : "1.0",
	developer     : "Outerthought",
	developer_url : "http://outerthought.org",
	c_owner       : "Outerthought",
	sponsor       : null,
	sponsor_url   : null,
	license       : "htmlArea"
};


DaisyImageUtils.prototype.insertImage = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    var parent = editor.getParentElement();
    if (parent != null && parent.tagName.toLowerCase() == "img") {
        var src = parent.getAttribute("daisy-src");
        if (src != undefined && src != "") {
            if (src.match(daisyUrlRegexp)) {
                daisyOpenLink(src, editor.daisyMountPoint, editor.daisySiteName);
                return;
            }
        }
        alert(DaisyImageUtils.I18N["move-cursor-of-image"]);
        return;
    }

    var dialogParams = { "mountPoint" : editor.daisyMountPoint, "siteName" : editor.daisySiteName,
        "branchId" : editor.daisyDocumentBranchId, "languageId" : editor.daisyDocumentLanguageId };

    daisy.dialog.popupDialog(daisyGetHtmlAreaDialog("DaisyImageUtils", "insert-image.html"),
        function(params) {
            var img;
            if (params.docId != undefined) {
                // params.url will be defined in case of imagebrowser but not in case of new image upload
                //  (could use some unifying of behaviour: UploadApple should get a branch/lang context similar to the image/docbrowsers)
                var src;
                if (params.url != null) {
                    src = params.url;
                } else {
                    src = "daisy:" + params.docId;
                    var branch = params.branchId != editor.daisyDocumentBranchId ? params.branch : "";
                    var language = params.languageId != editor.daisyDocumentLanguageId ? params.language : "";
                    if (branch != "" || language != "") {
                        src = src + "@" + branch
                        if (language != "")
                            src = src + ":" + language;
                    }
                }

                var version = params.version;
                if (version == null)
                    version = "live";

                var branch = params.branch;
                var language = params.language;

                if (branch == null)
                    branch = editor.daisyDocumentBranchId;
                if (language == null)
                    language = editor.daisyDocumentLanguageId;

                var url = daisy.mountPoint + "/" + daisy.site.name + "/" + params.docId + "/version/" + version + "/part/ImageData/data?branch=" + branch + "&language=" + language;

                img = editor._doc.createElement("img");
                img.setAttribute("src", url);
                img.setAttribute("daisy-src", src);
            } else {
                img = editor._doc.createElement("img");
                img.setAttribute("src", params.imageURL);
            }

            img = daisyInsertNode(img, editor);

            // Internet Explorer automatically assigns width and height attributes,
            // but we don't want that
            img.removeAttribute("width");
            img.removeAttribute("height");
        }, dialogParams);
}

DaisyImageUtils.prototype.alterImage = function(editor, id) {
    if (!editor.daisyIsEditingAllowed())
        return;

    var image = editor.getParentElement();
    if (!image || image.tagName.toLowerCase() != "img") {
        alert(DaisyImageUtils.I18N["js.select-image-first"]);
        return;
    }

    var dialogParams =
      {
        "align" : image.align,
        "width" : image.getAttribute("width"),
        "height" : image.getAttribute("height"),
        "printWidth" : image.getAttribute("print-width"),
        "printHeight" : image.getAttribute("print-height"),
        "caption" : image.getAttribute("daisy-caption"),
        "imageType" : image.getAttribute("daisy-image-type")
      };

    daisy.dialog.popupDialog(daisyGetHtmlAreaDialog("DaisyImageUtils", "image-settings.html"),
        function(params) {
            if (!params)
              return false;

            if (params.align != null && params.align != "")
              image.align = params.align;
            else
              image.removeAttribute("align");

            if (params.width != null && params.width != "")
              image.setAttribute("width", params.width);
            else
              image.removeAttribute("width");

            if (params.height != null && params.height != "")
              image.setAttribute("height", params.height);
            else
              image.removeAttribute("height");

            if (params.printWidth != null && params.printWidth != "")
              image.setAttribute("print-width", params.printWidth);
            else
              image.removeAttribute("print-width");

            if (params.printHeight != null && params.printHeight != "")
              image.setAttribute("print-height", params.printHeight);
            else
              image.removeAttribute("print-height");

            if (params.caption != null && params.caption != "")
              image.setAttribute("daisy-caption", params.caption);
            else
              image.removeAttribute("daisy-caption");

            if (params.imageType != null && params.imageType != "")
              image.setAttribute("daisy-image-type", params.imageType);
            else
              image.removeAttribute("daisy-image-type");
        }, dialogParams);
}

DaisyImageUtils.prototype.onUpdateToolbar = function() {
    var parent = this.editor.getParentElement();
    var parentName = parent.tagName.toLowerCase();
    if (parentName == "img") {
        this.editor._toolbarObjects["daisy-alter-image"].state("enabled", true, true);
    } else {
        this.editor._toolbarObjects["daisy-alter-image"].state("enabled", false, true);
    }
}