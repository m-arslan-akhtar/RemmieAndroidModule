# RTSP Server Integration


## JNI Module
- copy folder "gst-android-build" and paste it at [ptoject_name]>app>"gst-android-build"
- 
- copy "assets" folder and paste it at app>src>main>java>"assets"
- 
- create jni folder at app>src>main>"jni"
  - paste Android.mk,Application.mk, and .cpp files in it
  
- create directory "org.freedesktop.gstreamer" at app>src>main>java>"org.freedesktop.gstreamer"
  - copy androidmedia, Gstreamer.java, and tutorials folders in above directory
  - Register Tutorial2 Activity{app>src>main>java>org.freedesktop.gstreamer.tutorial.tutorial_2.Tutorial2.Java} in manifest and launch it from your desired location



## local.properties
- define NDK path "ndk.dir=[path]/sdk/ndk/21.4.7075529"

## gradle.properties
- define variable gstAndroidRoot and provide path to gstreamer, e.g. "gstAndroidRoot=/Users/dummy/Downloads/gstreamer-1.0-android-universal-1.22.1

## Build.gradle (app)
- -paste below code in defaultConfig{}
  -    externalNativeBuild {
  -         ndkBuild {
  -            def gstRoot
  -            if (project.hasProperty('gstAndroidRoot'))
  -                 gstRoot = project.gstAndroidRoot
  -            else
  -               gstRoot = System.env.GSTREAMER_ROOT_ANDROID
  -            if (gstRoot == null)
  -                 throw new Exception('GSTREAMER_ROOT_ANDROID must be set, or "gstAndroidRoot" must be defined in your gradle.properties in the top level directory of the unpacked universal GStreamer Android binaries')
  -             arguments "NDK_APPLICATION_MK=src/main/jni/Application.mk", "GSTREAMER_JAVA_SRC_DIR=src/main/java", "GSTREAMER_ROOT_ANDROID=$gstRoot", "GSTREAMER_ASSETS_DIR=src/main/assets"
  -            targets "tutorial-2"
  -            abiFilters 'arm64-v8a'
  -        }
  -  }

- Paste below code in android{}
  -  sourceSets {
  -    main {
  -      manifest.srcFile 'src/main/AndroidManifest.xml'
  -      java.srcDirs = ['src/main/java']
  -      jni.srcDirs = ['src/main/jni']
  -      aidl.srcDirs = ['src/main/aidl']
  -      res.srcDirs = ['src/main/res']
  -      assets.srcDirs = ['src/main/assets']
  -    }
  -  }

- Paste this code in android{}
  - externalNativeBuild {
  -   ndkBuild {
  -     path 'src/main/jni/Android.mk'
  -   }
  - }

- Paste below code at sibling of android{}, dependencies{}
   - afterEvaluate {
   -     compileDebugJavaWithJavac.dependsOn 'externalNativeBuildDebug'
   -     compileReleaseJavaWithJavac.dependsOn 'externalNativeBuildRelease'
   - }

## Manifest
- add INTERNET Permissions

# RemmieAndroidModule
