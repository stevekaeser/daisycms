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


var daisy;
if (!daisy)
    daisy = {};

if (!daisy.acleditor)
    daisy.acleditor = {};

// About the ACL editor:
//
//  The user can change the action for a certain permission by clicking
//  an icon which switches between the tree states (grant, deny, leave).
//
//  Clicking the icon changes the value of an invisible input field,
//  or the other way round: the icon represents the current value of
//  the input field.
//
//  Images which serve as ACL action switch have an attribute
//  aclActionSwitchFor.
//  The input field can optionally point to its switch icon with
//  the attribute aclActionSwitch. This is only needed when
//  programmatically updating the value of the input, so that the
//  icon can be kept in sync.
//  Lastly, the image can have an attribute onAclActionChange containing
//  code to be executed when the switch changes its state.

daisy.acleditor.init = function() {
    var imgs = document.getElementsByTagName("img");
    for (var i = 0; i < imgs.length; i++) {
        var img = imgs[i];
        var aclActionSwitchFor = img.getAttribute("aclActionSwitchFor");
        if (aclActionSwitchFor != null) {
            var value = document.getElementById(aclActionSwitchFor).value;
            daisy.acleditor.updateSwitchPermissionIcon(img, value);
            dojo.event.connect(img, "onclick", daisy.acleditor.callFunc(daisy.acleditor.switchAclPermission, img));
            daisy.acleditor.callOnChange(img);
        }
    }
}

daisy.acleditor.callOnChange = function(img) {
    var onchange = img.getAttribute("onAclActionChange");
    if (onchange != null && onchange != "")
        eval(onchange);
}

daisy.acleditor.callFunc = function(func, arg) {
    return function() { func.apply(null, [arg]) };
}

daisy.acleditor.switchAclPermission = function(image) {
    var daisyAclPermissions = [
            "org.outerj.daisy.repository.acl.AclActionType.GRANT",
            "org.outerj.daisy.repository.acl.AclActionType.DENY",
            "org.outerj.daisy.repository.acl.AclActionType.DO_NOTHING"];

    var aclActionSwitchFor = image.getAttribute("aclActionSwitchFor");
    var input = document.getElementById(aclActionSwitchFor);
    var currentState = input.value;
    var currentIndex = -1;

    for (var i = 0; i < daisyAclPermissions.length; i++) {
        if (currentState == daisyAclPermissions[i]) {
            currentIndex = i;
            break;
        }
    }

    if (currentIndex == -1) {
        alert("Not a valid ACL permission type: " + currentState);
        return;
    }

    var newIndex = (currentIndex + 1) % 3;
    var newState = daisyAclPermissions[newIndex];
    input.value = newState;

    daisy.acleditor.updateSwitchPermissionIcon(image, newState);
    daisy.acleditor.callOnChange(image);
}

daisy.acleditor.updateSwitchPermissionIcon = function(image, value) {
    var imagePath = daisy.mountPoint + "/resources/skins/default/images/";

    var permName;
    if (value == "org.outerj.daisy.repository.acl.AclActionType.GRANT") {
        permName = "grant";
    } else if (value == "org.outerj.daisy.repository.acl.AclActionType.DENY") {
        permName = "deny";
    } else if (value == "org.outerj.daisy.repository.acl.AclActionType.DO_NOTHING") {
        permName = "nothing";
    }

    image.src = imagePath + "acl_" + permName + ".gif";
}

daisy.acleditor.readAccessDetailGrantFields = ["radNonLive", "radLiveHistory", "radAllFields", "radAllParts",
    "radFullText", "radFTFragments", "radSummary"];
daisy.acleditor.readAccessDetailFields = daisy.acleditor.readAccessDetailGrantFields.concat(["radFields", "radParts"]);

daisy.acleditor.writeAccessDetailGrantFields = ["wadDocumentName", "wadLinks", "wadCustomFields", "wadCollections",
    "wadDocumentType", "wadRetired", "wadPrivate", "wadReferenceLanguage", "wadAllFields", "wadAllParts",
    "wadChangeComment", "wadChangeType", "wadSyncedWith", "wadVersionMeta"];
daisy.acleditor.writeAccessDetailFields = daisy.acleditor.writeAccessDetailGrantFields.concat(["wadFields", "wadParts"]);

daisy.acleditor.publishAccessDetailGrantFields = ["padLiveHistory"];
daisy.acleditor.publishAccessDetailFields = daisy.acleditor.publishAccessDetailGrantFields.concat([]);

daisy.acleditor.GRANT = "org.outerj.daisy.repository.acl.AclActionType.GRANT";
daisy.acleditor.DENY = "org.outerj.daisy.repository.acl.AclActionType.DENY";

daisy.acleditor.editReadAccessDetails = function(aclEntryId) {
    for (var i = 0; i < this.readAccessDetailFields.length; i++) {
        var field = this.readAccessDetailFields[i];
        var dialogField = dojo.byId(field);
        var storeField = dojo.byId(aclEntryId + "." + field + ":input");
        dialogField.value = storeField.value;

        var switchFor = dialogField.getAttribute("aclActionSwitch");
        if (switchFor != "" && switchFor != null) {
            daisy.acleditor.updateSwitchPermissionIcon(dojo.byId(switchFor), dialogField.value);
        }
    }

    dojo.byId('radParts').disabled = dojo.byId(aclEntryId + ".radAllParts:input").value != this.DENY;
    dojo.byId('radFields').disabled = dojo.byId(aclEntryId + ".radAllFields:input").value != this.DENY;

    var dialog = dojo.widget.byId("ReadAccessDetailsDialog");
    dialog.aclEntryId = aclEntryId;
    dialog.show();
}

daisy.acleditor.editWriteAccessDetails = function(aclEntryId) {
    for (var i = 0; i < this.writeAccessDetailFields.length; i++) {
        var field = this.writeAccessDetailFields[i];
        var dialogField = dojo.byId(field);
        var storeField = dojo.byId(aclEntryId + "." + field + ":input");
        dialogField.value = storeField.value;        

        var switchFor = dialogField.getAttribute("aclActionSwitch");
        if (switchFor != "" && switchFor != null) {
            daisy.acleditor.updateSwitchPermissionIcon(dojo.byId(switchFor), dialogField.value);
        }
    }

    dojo.byId('wadParts').disabled = dojo.byId(aclEntryId + ".wadAllParts:input").value != 'org.outerj.daisy.repository.acl.AclActionType.DENY';
    dojo.byId('wadFields').disabled = dojo.byId(aclEntryId + ".wadAllFields:input").value != 'org.outerj.daisy.repository.acl.AclActionType.DENY';

    var dialog = dojo.widget.byId("WriteAccessDetailsDialog");
    dialog.aclEntryId = aclEntryId;
    dialog.show();
}

daisy.acleditor.editPublishAccessDetails = function(aclEntryId) {
    for (var i = 0; i < this.publishAccessDetailFields.length; i++) {
        var field = this.publishAccessDetailFields[i];
        var dialogField = dojo.byId(field);
        var storeField = dojo.byId(aclEntryId + "." + field + ":input");
        dialogField.value = storeField.value;        

        var switchFor = dialogField.getAttribute("aclActionSwitch");
        if (switchFor != "" && switchFor != null) {
            daisy.acleditor.updateSwitchPermissionIcon(dojo.byId(switchFor), dialogField.value);
        }
    }

    var dialog = dojo.widget.byId("PublishAccessDetailsDialog");
    dialog.aclEntryId = aclEntryId;
    dialog.show();
}

daisy.acleditor.closeReadAccessDetailsDialog = function() {
    var dialog = dojo.widget.byId("ReadAccessDetailsDialog");
    var id = dialog.aclEntryId;

    for (var i = 0; i < this.readAccessDetailFields.length; i++) {
        var field = this.readAccessDetailFields[i];
        var dialogField = dojo.byId(field);
        var storeField = dojo.byId(id + "." + field + ":input");
        storeField.value = dialogField.value;
    }

    this.updateReadDetailsIcon(id);
}

daisy.acleditor.closeWriteAccessDetailsDialog = function() {
    var dialog = dojo.widget.byId("WriteAccessDetailsDialog");
    var id = dialog.aclEntryId;

    for (var i = 0; i < this.writeAccessDetailFields.length; i++) {
        var field = this.writeAccessDetailFields[i];
        var dialogField = dojo.byId(field);
        var storeField = dojo.byId(id + "." + field + ":input");
        storeField.value = dialogField.value;
    }

    this.updateWriteDetailsIcon(id);
}

daisy.acleditor.closePublishAccessDetailsDialog = function() {
    var dialog = dojo.widget.byId("PublishAccessDetailsDialog");
    var id = dialog.aclEntryId;

    for (var i = 0; i < this.publishAccessDetailFields.length; i++) {
        var field = this.publishAccessDetailFields[i];
        var dialogField = dojo.byId(field);
        var storeField = dojo.byId(id + "." + field + ":input");
        storeField.value = dialogField.value;
    }

    this.updatePublishDetailsIcon(id);
}

daisy.acleditor.updateReadDetailsIcon = function(aclEntryId) {
    var id = aclEntryId;
    var input = dojo.byId(id + ".readPerm");
    var img = dojo.byId(id + ".radEdit");

    if (input.value == this.GRANT) {
        // check whether any AccessDetails are specified, and change the details icon depending on this.
        var noAccessLimitations = true;
        for (var i = 0; i < this.readAccessDetailGrantFields.length; i++) {
            if (dojo.byId(id + "." + this.readAccessDetailGrantFields[i] + ":input").value != this.GRANT) {
                noAccessLimitations = false;
                break;
            }
        }

        var imgName = daisy.mountPoint + "/resources/skins/default/images/" + (noAccessLimitations ? "acl_accessdetailsdim.png" : "acl_accessdetails.png");

        img.src = imgName;
        img.style.display = "";
    } else {
        img.style.display = "none";
    }
}

daisy.acleditor.updateWriteDetailsIcon = function(aclEntryId) {
    var id = aclEntryId;
    var input = dojo.byId(id + ".writePerm");
    var img = dojo.byId(id + ".wadEdit");

    if (input.value == this.GRANT) {
        // check whether any AccessDetails are specified, and change the details icon depending on this.
        var noAccessLimitations = true;
        for (var i = 0; i < this.writeAccessDetailGrantFields.length; i++) {
            if (dojo.byId(id + "." + this.writeAccessDetailGrantFields[i] + ":input").value != this.GRANT) {
                noAccessLimitations = false;
                break;
            }
        }

        var imgName = daisy.mountPoint + "/resources/skins/default/images/" + (noAccessLimitations ? "acl_accessdetailsdim.png" : "acl_accessdetails.png");

        img.src = imgName;
        img.style.display = "";
    } else {
        img.style.display = "none";
    }
}

daisy.acleditor.updatePublishDetailsIcon = function(aclEntryId) {
    var id = aclEntryId;
    var input = dojo.byId(id + ".publishPerm");
    var img = dojo.byId(id + ".padEdit");

    if (input.value == this.GRANT) {
        // check whether any AccessDetails are specified, and change the details icon depending on this.
        var noAccessLimitations = true;
        for (var i = 0; i < this.publishAccessDetailGrantFields.length; i++) {
            if (dojo.byId(id + "." + this.publishAccessDetailGrantFields[i] + ":input").value != this.GRANT) {
                noAccessLimitations = false;
                break;
            }
        }

        var imgName = daisy.mountPoint + "/resources/skins/default/images/" + (noAccessLimitations ? "acl_accessdetailsdim.png" : "acl_accessdetails.png");

        img.src = imgName;
        img.style.display = "";
    } else {
        img.style.display = "none";
    }
}