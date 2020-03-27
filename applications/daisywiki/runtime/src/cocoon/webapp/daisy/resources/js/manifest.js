dojo.provide("daisy.manifest");

(function(){
    var map = {
        html: {
            "linkeditor": "daisy.widget.LinkEditor",
            "userselector": "daisy.widget.UserSelector",
            "doublelistboxpopup": "daisy.widget.DoubleListboxPopup"
            // register other widgets in daisy namespace here
        },
        svg: {
            // register svg widgets here
        },
        vml: {
            // register vml widgets here
        }
    };

    function formsResolver(name, domain){
        if(!domain){ domain="html"; }
        if(!map[domain]){ return null; }
        return map[domain][name];
    };

    dojo.registerNamespace("daisy", "daisy.widget", formsResolver);

})();