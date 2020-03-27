dojo.provide("daisy.html");

/*
 * Returns the value of the selected option in a HTML select control.
 */
daisy.html.selectedOptionValue = function(selectElement) {
    return selectElement.options[selectElement.selectedIndex].value;
}

/**
 * Selects the option of the specified select element that has the specified value.
 */
daisy.html.selectOption = function(selectElement, optionValue) {
    var options = selectElement.options;
    for (var i = 0; i < options.length; i++) {
        if (options[i].value == optionValue) {
            options[i].selected = true;
            return;
        }
    }
}

/**
 *  adds an option to a select element.
 */
daisy.html.addOption = function(selectElement, optionValue, optionText) {
  var opt = document.createElement("option");
  opt.text = optionText;
  opt.value = optionValue;
  
  try {
    selectElement.add(opt, null);
  } catch(ex) {
    selectElement.add(opt); // make IE happy
  }
  
  return opt;
}