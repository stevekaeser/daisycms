dojo.provide("daisy.util");

/*
 * Checks if two arrays are equal.
 */
daisy.util.nullSafeArraysEqual = function(array1, array2, compareFunction) {
    if (array1 == null && array2 == null)
        return true;

    if (array1 == null || array2 == null)
        return false;

    if (array1.length != array2.length)
        return false;

    for (var i = 0; i < array1.length; i++) {
        if (!compareFunction(array1[i], array2[i]))
            return false;
    }

    return true;
}

/**
 * Checks if the value occurs in the given array (identity test).
 */
daisy.util.arrayContains = function(anArray, value) {
    for (var i = 0; i < anArray.length; i++) {
        if (anArray[i] == value)
            return true;
    }
    return false;
}

daisy.util.urlRegexp = /^daisy:([0-9]{1,19}(?:-[a-zA-Z0-9_]{1,200})?)(?:@([^:#?]*)(?::([^:#?]*))?(?::([^:#?]*))?)?()(?:\?([^#]*))?(#.*)?$/;

daisy.util.parseDaisyLink = function(link) {
    if (link != null && link.match(daisy.util.urlRegexp)) {
        var result = {
                "documentId" : RegExp.$1,
                "branch" : RegExp.$2,
                "language" : RegExp.$3,
                "version": RegExp.$4,
                "queryString": RegExp.$6,
                "fragmentId": RegExp.$7
        };

        // normalization
        if (result.branch == "")
            result.branch = null;
        if (result.language == "")
            result.language = null;
        if (result.version == "")
            result.version = null;
        if (result.queryString == "")
            result.queryString = null;

        if (result.fragmentId != "" && result.fragmentId != null)
            result.fragmentId = result.fragmentId.substring(1);
        if (result.fragmentId == "")
            result.fragmentId = null;

        // parse query string
        if (result.queryString != null) {
            var queryParams = {};
            var nameValuePairs = result.queryString.split("&");
            for (var i = 0; i < nameValuePairs.length; i++) {
                var pair = nameValuePairs[i];
                var pos = pair.indexOf("=");
                var name;
                var value;
                if (pos != -1) {
                    name = pair.substring(0, pos);
                    value = pair.substring(pos + 1);
                } else {
                    name = pair;
                    value = "";
                }
                queryParams[name] = value;
            }
            result.queryParams = queryParams;
        } else {
            link.queryParams = {};
        }

        return result;
    } else {
        return null;
    }
}

/**
 * @param link a link object as returned by parseDaisyLink.
 */
daisy.util.formatDaisyLink = function(link) {
    var result = "daisy:" + link.documentId;
    if (link.branch != null || link.language != null || link.version != null) {
        result += "@";

        if (link.branch != null)
            result += link.branch;

        if (link.language != null || link.version != null) {
            result += ":";

            if (link.language != null)
                result += link.language;

            if (link.version != null) {
                result += ":" + link.version;
            }
        }
    }

    if (link.queryParams != null) {
        var queryString = "";

        for (var param in link.queryParams) {
            var value = link.queryParams[param];
            if (value != null) {
                if (queryString.length > 0)
                    queryString += "&";
                queryString += encodeURIComponent(param) + "=" + encodeURIComponent(value);
            }
        }

        if (queryString.length > 0)
            result += "?" + queryString;
    }

    if (link.fragmentId != null) {
        result += "#" + link.fragmentId;
    }

    return result;
}
