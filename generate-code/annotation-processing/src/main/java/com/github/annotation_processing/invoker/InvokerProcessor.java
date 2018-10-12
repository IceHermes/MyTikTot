package com.github.annotation_processing.invoker;

import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import com.github.annotation.invoker.ForInvoker;
import com.github.annotation.invoker.InvokeBy;
import com.github.annotation_processing.util.BaseProcessor;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import static com.github.annotation_processing.invoker.InvokerProcessor.INVOKER_CONFIG;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.github.annotation.invoker.InvokeBy")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(InvokerProcessor.INVOKER_CONFIG)
public class InvokerProcessor extends BaseProcessor {
  public static final String INVOKER_CONFIG = "invokerConfig";
  private String mFile;
  private Writer mWriter;
  
  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    mFile = processingEnv.getOptions().get(INVOKER_CONFIG);
  }
  
  @Override
  public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv) {
    Set<Invocation> invocations = new HashSet<>();
    for (Element method : roundEnv.getElementsAnnotatedWith(InvokeBy.class)) {
      if (method == null || method.getKind() != ElementKind.METHOD) {
        continue;
      }
      Set<Modifier> modifiers = method.getModifiers();
      if (!modifiers.contains(Modifier.PUBLIC) || !modifiers.contains(Modifier.STATIC)) {
        mMessager.printMessage(Diagnostic.Kind.ERROR,
            "方法必须是 public static的。" + method.getSimpleName(), method);
        continue;
      }
      if (((ExecutableElement) method).getParameters().size() != 0) {
        mMessager.printMessage(Diagnostic.Kind.ERROR,
            "方法必须是无参的。" + method.getSimpleName(), method);
        continue;
      }
      InvokeBy invokeBy = method.getAnnotation(InvokeBy.class);
      if (invokeBy == null) {
        continue;
      }
      Element clazz = method.getEnclosingElement();
      if (clazz == null || clazz.getKind() != ElementKind.CLASS) {
        continue;
      }
      Invocation invocation = new Invocation();
      invocation.mTarget = new InvokeMethod();
      invocation.mTarget.className = clazz.toString();
      invocation.mTarget.methodName = method.getSimpleName().toString();
      invocation.mInvoker = new InvokeMethod();
      try {
        invokeBy.invokerClass();
      } catch (MirroredTypeException e) {
        invocation.mInvoker.className = e.getTypeMirror().toString();
      }
      String methodId = invokeBy.methodId();
      Element invokerElement = mUtils.getTypeElement(invocation.mInvoker.className);
      for (Element invokerMethod : invokerElement.getEnclosedElements()) {
        if (invokerMethod == null || invokerMethod.getKind() != ElementKind.METHOD) {
          continue;
        }
        if (Optional.ofNullable(invokerMethod.getAnnotation(ForInvoker.class))
            .map(forInvoker -> forInvoker.methodId().equals(methodId)).orElse(false)) {
          invocation.mInvoker.methodName = invokerMethod.getSimpleName().toString();
        }
      }
      invocations.add(invocation);
    }
    
    if (invocations.isEmpty()) {
      return false;
    }
    boolean success = true;
    Writer writer = openWriter();
    try {
      Gson gson = new Gson();
      String json = gson.toJson(invocations);
      writer.append(json);
      writer.append("\n");
      writer.flush();
    } catch (IOException e) {
      success = false;
    }
    return success;
  }
  
  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    if (mWriter != null) {
      mWriter.close();
    }
  }
  
  private Writer openWriter() {
    if (mWriter != null) {
      return mWriter;
    }
    FileObject source = null;
    try {
      source = mFiler.createResource(StandardLocation.CLASS_OUTPUT, "",
          mFile);
      mWriter = source.openWriter();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return mWriter;
  }
}
