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
package org.outerj.daisy.doctaskrunner.serverimpl.actions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.outerj.daisy.doctaskrunner.DocTaskFailException;
import org.outerj.daisy.doctaskrunner.DocumentExecutionResult;
import org.outerj.daisy.doctaskrunner.TaskContext;
import org.outerj.daisy.doctaskrunner.TaskSpecification;
import org.outerj.daisy.doctaskrunner.spi.AbstractDocumentAction;
import org.outerj.daisy.repository.DocumentLockedException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;

public abstract class AbstractJavascriptDocumentAction extends AbstractDocumentAction {
    
    private Context context;
    private Script script;

    public void setup(VariantKey[] keys, TaskSpecification taskSpecification, TaskContext taskContext,
            Repository repository) throws Exception {
        super.setup(keys, taskSpecification, taskContext, repository);
        
        context = Context.enter();

        context.setOptimizationLevel(-1);
        taskContext.setProgress("Compiling script");
        
        onSetup(keys, taskSpecification, taskContext, repository);
        // start at line 0 since we wrapped the script in a function, this way we can deal with return values from the script
        this.script = context.compileString(wrapScript(getScriptCode()), "", 0, null); 
    }
    
    public void execute(VariantKey key, DocumentExecutionResult result) throws Exception{
        // create a new scope
        Scriptable scope = context.initStandardObjects();

        // make some stuff available to the script
        Object wrappedRepository = Context.javaToJS(repository, scope);
        ScriptableObject.putProperty(scope, "repository", wrappedRepository);
        Object wrappedKey = Context.javaToJS(key, scope);
        ScriptableObject.putProperty(scope, "variantKey", wrappedKey);

        Object returnValue;
        try {
            returnValue = script.exec(context, scope);
        } catch (JavaScriptException jse) {
            // We need to unwrap exceptions so that we can extract java exceptions thrown in the script
            // Actually this is to accommodate for the Fail exception that can be thrown 
            Throwable unwrappedException = jsExceptionToJavaException(jse);
            // these two exceptions are handled by the document task runner
            if (unwrappedException instanceof DocTaskFailException || unwrappedException instanceof DocumentLockedException) {
                throw (Exception)unwrappedException;
            } else {
                throw jse;
            }
        }
        if (returnValue instanceof Scriptable) {
            returnValue = jsToJava((Scriptable) returnValue);
        }
        // TODO great now what do we do with the return value ?
        String details = null;
        if (ScriptableObject.hasProperty(scope, "details")) {
            try {
                details = (String)Context.jsToJava(ScriptableObject.getProperty(scope, "details"), String.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("'details' could not be converted to a Java String", e);
            }
        }
        
        result.setMessage(details);
    }
    
    public void tearDown() throws Exception {
        onTearDown();
        Context.exit();
    }
    
    public abstract void onSetup(VariantKey[] variantKeys, TaskSpecification taskSpecification, TaskContext taskContext,
            Repository repository) throws Exception;

    public abstract String getScriptCode() throws Exception;

    public abstract void onTearDown() throws Exception;
    
    private String wrapScript(String script) {
        StringBuilder sb = new StringBuilder("function wrapScriptForReturn () {\n");
        sb.append(script).append("\n};\nwrapScriptForReturn();");
        return sb.toString();
    }
    
    private Object jsToJava(Scriptable so) {
        Object retVal;
        if (so instanceof NativeObject) {
            Map m  = new HashMap();
            for (Object key : ScriptableObject.getPropertyIds(so)) {
                Object val = ScriptableObject.getProperty(so, (String)key);
                if (val instanceof Scriptable) {
                    val = jsToJava((Scriptable)val);
                }
                m.put(key, val);
            }
            retVal = m;
        }else if (so instanceof NativeArray) {            
            NativeArray na = (NativeArray)so;
            Object[] a = new Object[(int)na.getLength()];
            for(int i = 0; i < a.length; i++) {
                Object val = na.get(i, na);
                if (val instanceof Scriptable) {
                    val = jsToJava(na);
                }
                a[i] = val;
            }
            retVal = a;
        } else if ("Date".equals(so.getClassName())) {
            retVal = Context.jsToJava(so, Date.class);
        } else if (so instanceof NativeJavaObject) {
            retVal = ((NativeJavaObject)so).unwrap(); 
        } else {
            retVal = so;
        }
        return retVal;
    }

    private Throwable jsExceptionToJavaException (JavaScriptException e){
        Throwable javaThrowable = e;

        // See if it's nontrivally wrapping either an exception
        // thrown from Java method (in which case it's a NativeError
        // wrapping a NativeJavaObject, wrapping a Throwable) or a Java
        // exception directly thrown in the script (i.e.
        // "throw new java.lang.Exception()" executed in script), in
        // which case it is a NativeJavaObject wrapping a Throwable.

        Object val = e.getValue();

        // Unwrap NativeError to NativeJavaObject first, if possible
        if(val instanceof Scriptable)
        {
            Object njo = ScriptableObject.getProperty(((Scriptable)val),
                    "rhinoException");
            if(njo instanceof NativeJavaObject)
            {
                val = njo;
            }
            else
            {
                njo = ScriptableObject.getProperty(((Scriptable)val),
                    "javaException");
                if(njo instanceof NativeJavaObject)
                {
                    val = njo;
                }
            }
        }
        // If val is a NativeJavaObject, unwrap to the Java object
        if(val instanceof NativeJavaObject)
        {
            val = ((NativeJavaObject)val).unwrap();
        }
        // If val is now a Throwable, it's the one we were looking for.
        // Otherwise, it'll remain set to the JavaScriptException
        if(val instanceof Throwable)
        {
            javaThrowable = (Throwable)val;
        }
        
        return javaThrowable;
    }
    
}