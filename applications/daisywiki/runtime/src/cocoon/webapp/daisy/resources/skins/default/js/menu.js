function daisyMenuInit() {
  if (document.all && document.getElementById) {
    navRoot = document.getElementById("generalNavigation");
    if (navRoot == null)
      return;
    navRoot = navRoot.getElementsByTagName('UL')[0];
    for (i=0; i<navRoot.childNodes.length; i++) {
      node = navRoot.childNodes[i];
      if (node.nodeName=="LI") {
        node.onmouseover=function() {
          this.className+=" over";
        }
        node.onmouseout=function() {
          this.className=this.className.replace(" over", "");
        }
      }
    }
  }
}

daisyPushOnLoad(daisyMenuInit);