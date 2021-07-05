package com.leslie.task_compiler;

import com.google.auto.service.AutoService;
import com.leslie.task_annotation.Constant;
import com.leslie.task_annotation.ITask;
import com.leslie.task_annotation.Task;
import com.leslie.task_annotation.TaskMeta;
import com.leslie.task_annotation.TaskType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * 作者：xjzhao
 * 时间：2021-06-29 16:47
 */
@AutoService(Processor.class)
@SupportedOptions("InitTask") //处理器接受参数
@SupportedSourceVersion(SourceVersion.RELEASE_8) //指定java版本
@SupportedAnnotationTypes({"com.leslie.annotation.Task"}) //处理的注解
public class TaskProcessor extends AbstractProcessor {

    // 操作Element的工具类（类，函数，属性，其实都是Element）
    private Elements elementTool;

    // type(类信息)的工具类，包含用于操作TypeMirror的工具方法
    private Types typeTool;

    // Message用来打印 日志相关信息
    private Messager messager;

    // 文件生成器， 类 资源 等，就是最终要生成的文件 是需要Filer来完成的
    private Filer filer;

    // module名字
    private String moduleName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        messager = processingEnvironment.getMessager();
        elementTool = processingEnvironment.getElementUtils();
        filer = processingEnvironment.getFiler();

        //参数是模块名 为了防止多模块/组件化开发的时候 生成相同的 xx$$ROOT$$文件
        Map<String, String> options = processingEnvironment.getOptions();
        if (!options.isEmpty()) {
            moduleName = options.get(Constant.ARGUMENTS_NAME);
        }
        messager.printMessage(Diagnostic.Kind.NOTE, "TaskProcessor::*************init 开启处理Task相关注解*************");
    }


    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.isEmpty()) return false;
        parse(roundEnvironment.getElementsAnnotatedWith(Task.class));
        return true;
    }


    private void parse(Set<? extends Element> elements) {
        if (null == moduleName || "".equals(moduleName)){
            throw new AssertionError(Constant.NO_MODULE_NAME_TIPS);
        }

        // protected final List<TaskMeta> getTasks(){}
        ParameterizedTypeName returnList = ParameterizedTypeName.get(
                ClassName.get(List.class),
                ClassName.get(TaskMeta.class)
        );
        MethodSpec.Builder getTasksBuilder = MethodSpec.methodBuilder(Constant.METHOD_NAME)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(returnList)
                .addStatement("List<TaskMeta> list = new java.util.ArrayList<TaskMeta>()");




        for (Element element : elements) {

            String packageName = elementTool.getPackageOf(element).getQualifiedName().toString();

            // 获取简单类名
            String className = element.getSimpleName().toString();
            messager.printMessage(Diagnostic.Kind.NOTE, "被@Task注解的类有：" + packageName + "." + className);


            Task task = element.getAnnotation(Task.class);
            // list中添加元素
            getTasksBuilder.addStatement(
                    "list.add($T.build($T." + task.thread() + "," + task.priority() + "," + task.delayMillis() + ", $T.class" + "))",
                    TaskMeta.class,
                    TaskType.class,
                    ClassName.get((TypeElement) element));
        }

        getTasksBuilder.addStatement("return list");

        // _InitTask$$InitTask.class

        TypeSpec _InitTask$$InitTask = TypeSpec.classBuilder(Constant.FILE_NAME_START + moduleName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(ITask.class)
                .addMethod(getTasksBuilder.build())
                .build();

        try {
            JavaFile javaFile = JavaFile.builder(Constant.PACKAGE, _InitTask$$InitTask)
                    .addFileComment(" This codes are generated automatically. Do not modify!")
                    .build();
            // write to file
            javaFile.writeTo(filer);

            messager.printMessage(Diagnostic.Kind.NOTE, "InitTask::代码已生成！");

        } catch (IOException e) {
            e.printStackTrace();
            messager.printMessage(Diagnostic.Kind.NOTE, "InitTask::生成代码失败");

        }


    }
}
