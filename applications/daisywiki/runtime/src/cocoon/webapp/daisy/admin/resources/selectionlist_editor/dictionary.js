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

// Simple dictionary class (lost too much time on the dojo one which did not seem to work, API is similar to dojo one)

function DaisyDictionary() {
    this.items = {};
}

DaisyDictionary.prototype.item = function(key) {
    return this.items[key];
}

DaisyDictionary.prototype.clear = function() {
    this.items = {};
}

DaisyDictionary.prototype.getKeyList = function() {
    var keys = [];
    for (var item in this.items) {
        keys.push(item);
    }
    return keys;
}

DaisyDictionary.prototype.add = function(key, value) {
    this.items[key] = value;
}

DaisyDictionary.prototype.remove = function(key) {
    delete this.items[key];
}

DaisyDictionary.prototype.clone = function() {
    var newDict = new DaisyDictionary();
    for (var item in this.items) {
        newDict.add(item, this.items[item]);
    }
    return newDict;
}

DaisyDictionary.prototype.equals = function(dict2) {
    var dict1Keys = this.getKeyList();
    if (dict1Keys.length != dict2.getKeyList().length)
        return false;

    for (var i = 0; i < dict1Keys.length; i++) {
        var val1 = this.item(dict1Keys[i]);
        var val2 = dict2.item(dict1Keys[i]);
        if (val1 != val2)
            return false;
    }

    return true;
}
