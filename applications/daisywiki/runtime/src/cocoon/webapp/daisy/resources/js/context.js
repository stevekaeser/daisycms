dojo.provide("daisy.context");

/*
 * Gets a resource located in the javascript.
 */
daisy.context.getResourcePath = function(path) {
    return daisy.mountPoint + "/resources/js/" + path;
}

/*
 * Gets a resource located in the javascript as dojo uri.
 */
daisy.context.getResourceUri = function(path) {
    return dojo.uri.dojoUri(this.getResourcePath(path));
}
