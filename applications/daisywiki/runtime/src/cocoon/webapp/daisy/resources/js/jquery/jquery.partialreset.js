;(function($) {
    
$.fn.extend({
    partialReset: function() {
        return this.each(function() {
            if ($(this).is("input:text,input:hidden,input:password")) {
                this.value = this.defaultValue;
              } if ($(this).is("input:checkbox")) {
                this.checked = this.defaultChecked;
              } else if ($(this).is("input:radio")) {
                this.checked = this.defaultChecked;
              } else if ($(this).is("select")) {
                $(this).children("option").each(function() {
                  this.selected = this.defaultSelected;
                });
              }
        });
    },
    formChanged : function formChanged(form) {
        for (var i = 0; i < form.elements.length; i++) {
            if(form.elements[i].value != form.elements[i].defaultValue) return(true);
        }
        return(false);
    }
});

})(jQuery);
