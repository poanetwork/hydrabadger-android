use android_c_headers::*;
use Session;

use OnEvent;








#[allow(dead_code)]
trait SwigInto<T> {
    fn swig_into(self, env: *mut JNIEnv)
    -> T;
}
#[allow(dead_code)]
trait SwigFrom<T> {
    fn swig_from(T, env: *mut JNIEnv)
    -> Self;
}
#[allow(dead_code)]
trait SwigDeref {
    type
    Target: ?Sized;
    fn swig_deref(&self)
    -> &Self::Target;
}
#[allow(dead_code)]
trait SwigDerefMut {
    type
    Target: ?Sized;
    fn swig_deref_mut(&mut self)
    -> &mut Self::Target;
}
#[allow(dead_code)]
trait SwigForeignClass {
    fn jni_class_name()
    -> *const ::std::os::raw::c_char;
    fn box_object(x: Self)
    -> jlong;
}
#[allow(unused_macros)]
macro_rules! swig_c_str(( $ lit : expr ) => {
                        concat ! ( $ lit , "\0" ) . as_ptr (  ) as * const ::
                        std :: os :: raw :: c_char } ;);
#[allow(unused_macros)]
macro_rules! swig_assert_eq_size(( $ x : ty , $ ( $ xs : ty ) , + $ ( , ) * )
                                 => {
                                 # [
                                 allow (
                                 unknown_lints , forget_copy , unused_unsafe ,
                                 useless_transmute ) ] unsafe {
                                 use std :: mem :: {
                                 forget , transmute , uninitialized } ; $ (
                                 forget :: < $ xs > (
                                 transmute ( uninitialized :: < $ x > (  ) ) )
                                 ; ) + } } ;);
#[cfg(target_pointer_width = "32")]
pub unsafe fn jlong_to_pointer<T>(val: jlong) -> *mut T {
    (val as u32) as *mut T
}
#[allow(dead_code)]
pub struct JavaString {
    string: jstring,
    chars: *const ::std::os::raw::c_char,
    env: *mut JNIEnv,
}
#[allow(dead_code)]
impl JavaString {
    pub fn new(env: *mut JNIEnv, js: jstring) -> JavaString {
        let chars =
            if !js.is_null() {
                unsafe {
                    (**env).GetStringUTFChars.unwrap()(env, js,
                                                       ::std::ptr::null_mut())
                }
            } else { ::std::ptr::null_mut() };
        JavaString{string: js, chars: chars, env: env,}
    }
    pub fn to_str(&self) -> &str {
        if !self.chars.is_null() {
            let s = unsafe { ::std::ffi::CStr::from_ptr(self.chars) };
            s.to_str().unwrap()
        } else { "" }
    }
}
#[allow(dead_code)]
impl Drop for JavaString {
    fn drop(&mut self) {
        assert!(! self . env . is_null (  ));
        if !self.string.is_null() {
            assert!(! self . chars . is_null (  ));
            unsafe {
                (**self.env).ReleaseStringUTFChars.unwrap()(self.env,
                                                            self.string,
                                                            self.chars)
            };
            self.env = ::std::ptr::null_mut();
            self.chars = ::std::ptr::null_mut();
        }
    }
}
#[allow(dead_code)]
struct JavaCallback {
    java_vm: *mut JavaVM,
    this: jobject,
    methods: Vec<jmethodID>,
}
#[allow(dead_code)]
struct JniEnvHolder<'a> {
    env: Option<*mut JNIEnv>,
    callback: &'a JavaCallback,
    need_detach: bool,
}
#[allow(dead_code)]
impl <'a> Drop for JniEnvHolder<'a> {
    fn drop(&mut self) {
        if self.need_detach {
            let res =
                unsafe {
                    (**self.callback.java_vm).DetachCurrentThread.unwrap()(self.callback.java_vm)
                };
            if res != 0 {
                error!("JniEnvHolder: DetachCurrentThread failed: {}" , res);
            }
        }
    }
}
#[allow(dead_code)]
impl JavaCallback {
    fn new(obj: jobject, env: *mut JNIEnv) -> JavaCallback {
        let mut java_vm: *mut JavaVM = ::std::ptr::null_mut();
        let ret = unsafe { (**env).GetJavaVM.unwrap()(env, &mut java_vm) };
        assert_eq!(0 , ret , "GetJavaVm failed");
        let global_obj = unsafe { (**env).NewGlobalRef.unwrap()(env, obj) };
        assert!(! global_obj . is_null (  ));
        JavaCallback{java_vm, this: global_obj, methods: Vec::new(),}
    }
    fn get_jni_env(&self) -> JniEnvHolder {
        assert!(! self . java_vm . is_null (  ));
        let mut env: *mut JNIEnv = ::std::ptr::null_mut();
        #[cfg(target_os = "android")]
        type GetJNiEnvPtrPtr = *mut *mut JNIEnv;
        #[cfg(not(target_os = "android"))]
        type GetJNiEnvPtrPtr = *mut *mut ::std::os::raw::c_void;
        let res =
            unsafe {
                (**self.java_vm).GetEnv.unwrap()(self.java_vm,
                                                 (&mut env) as
                                                     *mut *mut JNIEnv as
                                                     *mut *mut ::std::os::raw::c_void,
                                                 JNI_VERSION_1_6 as jint)
            };
        if res == (JNI_OK as jint) {
            return JniEnvHolder{env: Some(env),
                                callback: self,
                                need_detach: false,};
        }
        if res != (JNI_EDETACHED as jint) {
            panic!("get_jni_env: GetEnv return error `{}`" , res);
        }
        let res =
            unsafe {
                (**self.java_vm).AttachCurrentThread.unwrap()(self.java_vm,
                                                              (&mut env) as
                                                                  *mut *mut JNIEnv
                                                                  as
                                                                  GetJNiEnvPtrPtr,
                                                              ::std::ptr::null_mut())
            };
        if res != 0 {
            error!("JavaCallback::get_jnienv: AttachCurrentThread failed: {}"
                   , res);
            JniEnvHolder{env: None, callback: self, need_detach: false,}
        } else {
            assert!(! env . is_null (  ));
            JniEnvHolder{env: Some(env), callback: self, need_detach: true,}
        }
    }
}
#[allow(dead_code)]
impl Drop for JavaCallback {
    fn drop(&mut self) {
        let env = self.get_jni_env();
        if let Some(env) = env.env {
            assert!(! env . is_null (  ));
            unsafe { (**env).DeleteGlobalRef.unwrap()(env, self.this) };
        } else { error!("JavaCallback::drop failed, can not get JNIEnv"); }
    }
}
#[allow(dead_code)]
fn jni_throw(env: *mut JNIEnv, class_name: *const ::std::os::raw::c_char,
             message: &str) {
    let ex_class = unsafe { (**env).FindClass.unwrap()(env, class_name) };
    if ex_class.is_null() {
        error!("throw_exception: can not find exp class {:?}, msg {}" , unsafe
               { :: std :: ffi :: CStr :: from_ptr ( class_name ) } ,
               message);
        return;
    }
    let c_message = ::std::ffi::CString::new(message).unwrap();
    let res =
        unsafe {
            (**env).ThrowNew.unwrap()(env, ex_class, c_message.as_ptr())
        };
    if res != 0 {
        error!("ThrowNew({}) for class {:?} failed" , message , unsafe {
               :: std :: ffi :: CStr :: from_ptr ( class_name ) });
    }
}
#[allow(dead_code)]
fn jni_throw_exception(env: *mut JNIEnv, message: &str) {
    jni_throw(env, swig_c_str!("java/lang/Exception"), message)
}
#[allow(dead_code)]
fn object_to_jobject<T: SwigForeignClass>(obj: T,
                                          class_id:
                                              *const ::std::os::raw::c_char,
                                          env: *mut JNIEnv) -> jobject {
    let jcls: jclass = unsafe { (**env).FindClass.unwrap()(env, class_id) };
    assert!(! jcls . is_null (  ) , "object_to_jobject: FindClass failed");
    let jobj: jobject = unsafe { (**env).AllocObject.unwrap()(env, jcls) };
    assert!(! jobj . is_null (  ) , "object_to_jobject: AllocObject failed");
    let field_id: jfieldID =
        unsafe {
            (**env).GetFieldID.unwrap()(env, jcls, swig_c_str!("mNativeObj"),
                                        swig_c_str!("J"))
        };
    assert!(! field_id . is_null (  ) ,
            "object_to_jobject: GetFieldID(mNativeObj) failed");
    let ret: jlong = <T>::box_object(obj);
    unsafe {
        (**env).SetLongField.unwrap()(env, jobj, field_id, ret);
        if (**env).ExceptionCheck.unwrap()(env) != 0 {
            panic!("object_to_jobject: Can not set mNativeObj field: catch exception");
        }
    }
    jobj
}
#[allow(dead_code)]
fn vec_of_objects_to_jobject_array<T: SwigForeignClass>(mut arr: Vec<T>,
                                                        class_id:
                                                            *const ::std::os::raw::c_char,
                                                        env: *mut JNIEnv)
 -> jobjectArray {
    let jcls: jclass = unsafe { (**env).FindClass.unwrap()(env, class_id) };
    assert!(! jcls . is_null (  ));
    let obj_arr: jobjectArray =
        unsafe {
            (**env).NewObjectArray.unwrap()(env, arr.len() as jsize, jcls,
                                            ::std::ptr::null_mut())
        };
    assert!(! obj_arr . is_null (  ));
    let field_id = swig_c_str!("mNativeObj");
    let type_id = swig_c_str!("J");
    let field_id: jfieldID =
        unsafe { (**env).GetFieldID.unwrap()(env, jcls, field_id, type_id) };
    assert!(! field_id . is_null (  ));
    for (i, r_obj) in arr.drain(..).enumerate() {
        let jobj: jobject =
            unsafe { (**env).AllocObject.unwrap()(env, jcls) };
        assert!(! jobj . is_null (  ));
        let r_obj: jlong = <T>::box_object(r_obj);
        unsafe {
            (**env).SetLongField.unwrap()(env, jobj, field_id, r_obj);
            if (**env).ExceptionCheck.unwrap()(env) != 0 {
                panic!("Can not mNativeObj field: catch exception");
            }
            (**env).SetObjectArrayElement.unwrap()(env, obj_arr, i as jsize,
                                                   jobj);
            if (**env).ExceptionCheck.unwrap()(env) != 0 {
                panic!("SetObjectArrayElement({}) failed" , i);
            }
            (**env).DeleteLocalRef.unwrap()(env, jobj);
        }
    }
    obj_arr
}
#[allow(dead_code)]
trait JniInvalidValue<T> {
    fn invalid_value()
    -> T;
}
impl <T> JniInvalidValue<*const T> for *const T {
    fn invalid_value() -> *const T { ::std::ptr::null() }
}
impl <T> JniInvalidValue<*mut T> for *mut T {
    fn invalid_value() -> *mut T { ::std::ptr::null_mut() }
}
impl JniInvalidValue<()> for () {
    fn invalid_value() { }
}
macro_rules! impl_jni_invalid_value(( $ ( $ type : ty ) * ) => (
                                    $ (
                                    impl JniInvalidValue < $ type > for $ type
                                    {
                                    fn invalid_value (  ) -> $ type {
                                    < $ type > :: default (  ) } } ) * ));
impl_jni_invalid_value! (jbyte jshort jint jlong jfloat jdouble);
macro_rules! define_array_handling_code((
                                        $ (
                                        [
                                        jni_arr_type = $ jni_arr_type : ident
                                        , rust_arr_wrapper = $
                                        rust_arr_wrapper : ident ,
                                        jni_get_array_elements = $
                                        jni_get_array_elements : ident ,
                                        jni_elem_type = $ jni_elem_type :
                                        ident , rust_elem_type = $
                                        rust_elem_type : ident ,
                                        jni_release_array_elements = $
                                        jni_release_array_elements : ident ,
                                        jni_new_array = $ jni_new_array :
                                        ident , jni_set_array_region = $
                                        jni_set_array_region : ident ] ) , * )
                                        => {
                                        $ (
                                        # [ allow ( dead_code ) ] struct $
                                        rust_arr_wrapper {
                                        array : $ jni_arr_type , data : * mut
                                        $ jni_elem_type , env : * mut JNIEnv ,
                                        } # [ allow ( dead_code ) ] impl $
                                        rust_arr_wrapper {
                                        fn new (
                                        env : * mut JNIEnv , array : $
                                        jni_arr_type ) -> $ rust_arr_wrapper {
                                        assert ! ( ! array . is_null (  ) ) ;
                                        let data = unsafe {
                                        ( * * env ) . $ jni_get_array_elements
                                        . unwrap (  ) (
                                        env , array , :: std :: ptr ::
                                        null_mut (  ) ) } ; $ rust_arr_wrapper
                                        { array , data , env } } fn to_slice (
                                        & self ) -> & [ $ rust_elem_type ] {
                                        unsafe {
                                        let len : jsize = ( * * self . env ) .
                                        GetArrayLength . unwrap (  ) (
                                        self . env , self . array ) ; assert !
                                        (
                                        ( len as u64 ) <= (
                                        usize :: max_value (  ) as u64 ) ) ;
                                        :: std :: slice :: from_raw_parts (
                                        self . data , len as usize ) } } fn
                                        from_slice_to_raw (
                                        arr : & [ $ rust_elem_type ] , env : *
                                        mut JNIEnv ) -> $ jni_arr_type {
                                        assert ! (
                                        ( arr . len (  ) as u64 ) <= (
                                        jsize :: max_value (  ) as u64 ) ) ;
                                        let jarr : $ jni_arr_type = unsafe {
                                        ( * * env ) . $ jni_new_array . unwrap
                                        (  ) ( env , arr . len (  ) as jsize )
                                        } ; assert ! ( ! jarr . is_null (  ) )
                                        ; unsafe {
                                        ( * * env ) . $ jni_set_array_region .
                                        unwrap (  ) (
                                        env , jarr , 0 , arr . len (  ) as
                                        jsize , arr . as_ptr (  ) ) ; if (
                                        * * env ) . ExceptionCheck . unwrap (
                                        ) ( env ) != 0 {
                                        panic ! (
                                        "{}:{} {} failed" , file ! (  ) , line
                                        ! (  ) , stringify ! (
                                        $ jni_set_array_region ) ) ; } } jarr
                                        } } # [ allow ( dead_code ) ] impl
                                        Drop for $ rust_arr_wrapper {
                                        fn drop ( & mut self ) {
                                        assert ! ( ! self . env . is_null (  )
                                        ) ; assert ! (
                                        ! self . array . is_null (  ) ) ;
                                        unsafe {
                                        ( * * self . env ) . $
                                        jni_release_array_elements . unwrap (
                                        ) (
                                        self . env , self . array , self .
                                        data , JNI_ABORT as jint , ) } ; } } )
                                        * });
define_array_handling_code! ([
                             jni_arr_type = jbyteArray , rust_arr_wrapper =
                             JavaByteArray , jni_get_array_elements =
                             GetByteArrayElements , jni_elem_type = jbyte ,
                             rust_elem_type = i8 , jni_release_array_elements
                             = ReleaseByteArrayElements , jni_new_array =
                             NewByteArray , jni_set_array_region =
                             SetByteArrayRegion ] , [
                             jni_arr_type = jshortArray , rust_arr_wrapper =
                             JavaShortArray , jni_get_array_elements =
                             GetShortArrayElements , jni_elem_type = jshort ,
                             rust_elem_type = i16 , jni_release_array_elements
                             = ReleaseShortArrayElements , jni_new_array =
                             NewShortArray , jni_set_array_region =
                             SetShortArrayRegion ] , [
                             jni_arr_type = jintArray , rust_arr_wrapper =
                             JavaIntArray , jni_get_array_elements =
                             GetIntArrayElements , jni_elem_type = jint ,
                             rust_elem_type = i32 , jni_release_array_elements
                             = ReleaseIntArrayElements , jni_new_array =
                             NewIntArray , jni_set_array_region =
                             SetIntArrayRegion ] , [
                             jni_arr_type = jlongArray , rust_arr_wrapper =
                             JavaLongArray , jni_get_array_elements =
                             GetLongArrayElements , jni_elem_type = jlong ,
                             rust_elem_type = i64 , jni_release_array_elements
                             = ReleaseLongArrayElements , jni_new_array =
                             NewLongArray , jni_set_array_region =
                             SetLongArrayRegion ] , [
                             jni_arr_type = jfloatArray , rust_arr_wrapper =
                             JavaFloatArray , jni_get_array_elements =
                             GetFloatArrayElements , jni_elem_type = jfloat ,
                             rust_elem_type = f32 , jni_release_array_elements
                             = ReleaseFloatArrayElements , jni_new_array =
                             NewFloatArray , jni_set_array_region =
                             SetFloatArrayRegion ] , [
                             jni_arr_type = jdoubleArray , rust_arr_wrapper =
                             JavaDoubleArray , jni_get_array_elements =
                             GetDoubleArrayElements , jni_elem_type = jdouble
                             , rust_elem_type = f64 ,
                             jni_release_array_elements =
                             ReleaseDoubleArrayElements , jni_new_array =
                             NewDoubleArray , jni_set_array_region =
                             SetDoubleArrayRegion ]);
impl SwigFrom<bool> for jboolean {
    fn swig_from(x: bool, _: *mut JNIEnv) -> Self {
        if x { 1 as jboolean } else { 0 as jboolean }
    }
}
impl SwigFrom<String> for jstring {
    fn swig_from(x: String, env: *mut JNIEnv) -> Self {
        let x = x.into_bytes();
        let x = unsafe { ::std::ffi::CString::from_vec_unchecked(x) };
        unsafe { (**env).NewStringUTF.unwrap()(env, x.as_ptr()) }
    }
}
impl OnEvent for JavaCallback {
    #[allow(unused_mut)]
    fn changed(&self, a_0: bool, a_1: String, a_2: String) {
        swig_assert_eq_size!(:: std :: os :: raw :: c_uint , u32);
        swig_assert_eq_size!(:: std :: os :: raw :: c_int , i32);
        let env = self.get_jni_env();
        if let Some(env) = env.env {
            let mut a_0: jboolean = <jboolean>::swig_from(a_0, env);
            let mut a_1: jstring = <jstring>::swig_from(a_1, env);
            let mut a_2: jstring = <jstring>::swig_from(a_2, env);
            unsafe {
                (**env).CallVoidMethod.unwrap()(env, self.this,
                                                self.methods[0],
                                                a_0 as ::std::os::raw::c_uint,
                                                a_1, a_2);
                if (**env).ExceptionCheck.unwrap()(env) != 0 {
                    error!("changed: java throw exception");
                    (**env).ExceptionDescribe.unwrap()(env);
                    (**env).ExceptionClear.unwrap()(env);
                }
            };
        }
    }
}
impl SwigForeignClass for Session {
    fn jni_class_name() -> *const ::std::os::raw::c_char {
        swig_c_str!("net/korul/hbbft/Session")
    }
    fn box_object(this: Self) -> jlong {
        let this: Box<Session> = Box::new(this);
        let this: *mut Session = Box::into_raw(this);
        this as jlong
    }
}
#[no_mangle]
#[allow(unused_variables, unused_mut, non_snake_case)]
pub extern "C" fn Java_net_korul_hbbft_Session_init(env: *mut JNIEnv,
                                                    _: jclass) -> jlong {
    let this: Session = Session::new();
    let this: Box<Session> = Box::new(this);
    let this: *mut Session = Box::into_raw(this);
    this as jlong
}
impl SwigInto<i32> for jint {
    fn swig_into(self, _: *mut JNIEnv) -> i32 { self }
}
impl SwigInto<JavaString> for jstring {
    fn swig_into(self, env: *mut JNIEnv) -> JavaString {
        JavaString::new(env, self)
    }
}
impl SwigDeref for JavaString {
    type
    Target
    =
    str;
    fn swig_deref(&self) -> &Self::Target { self.to_str() }
}
impl <'a> SwigInto<String> for &'a str {
    fn swig_into(self, _: *mut JNIEnv) -> String { self.into() }
}
#[allow(non_snake_case, unused_variables, unused_mut)]
#[no_mangle]
pub extern "C" fn Java_net_korul_hbbft_Session_do_1send_1message(env:
                                                                     *mut JNIEnv,
                                                                 _: jclass,
                                                                 this: jlong,
                                                                 a_0: jint,
                                                                 a_1: jstring)
 -> () {
    let mut a_0: i32 = a_0.swig_into(env);
    let mut a_1: JavaString = a_1.swig_into(env);
    let mut a_1: &str = a_1.swig_deref();
    let mut a_1: String = a_1.swig_into(env);
    let this: &Session =
        unsafe { jlong_to_pointer::<Session>(this).as_mut().unwrap() };
    let mut ret: () = Session::send_message(this, a_0, a_1);
    ret
}
#[allow(non_snake_case, unused_variables, unused_mut)]
#[no_mangle]
pub extern "C" fn Java_net_korul_hbbft_Session_do_1start_1node(env:
                                                                   *mut JNIEnv,
                                                               _: jclass,
                                                               this: jlong,
                                                               a_0: jstring,
                                                               a_1: jstring,
                                                               a_2: jstring)
 -> () {
    let mut a_0: JavaString = a_0.swig_into(env);
    let mut a_0: &str = a_0.swig_deref();
    let mut a_0: String = a_0.swig_into(env);
    let mut a_1: JavaString = a_1.swig_into(env);
    let mut a_1: &str = a_1.swig_deref();
    let mut a_1: String = a_1.swig_into(env);
    let mut a_2: JavaString = a_2.swig_into(env);
    let mut a_2: &str = a_2.swig_deref();
    let mut a_2: String = a_2.swig_into(env);
    let this: &Session =
        unsafe { jlong_to_pointer::<Session>(this).as_mut().unwrap() };
    let mut ret: () = Session::start_node(this, a_0, a_1, a_2);
    ret
}
impl SwigFrom<jobject> for Box<OnEvent> {
    fn swig_from(this: jobject, env: *mut JNIEnv) -> Self {
        let mut cb = JavaCallback::new(this, env);
        cb.methods.reserve(1);
        let class = unsafe { (**env).GetObjectClass.unwrap()(env, cb.this) };
        assert!(! class . is_null (  ) ,
                "GetObjectClass return null class for MyObserver");
        let method_id: jmethodID =
            unsafe {
                (**env).GetMethodID.unwrap()(env, class,
                                             swig_c_str!("onStateChanged"),
                                             swig_c_str!("(ZLjava/lang/String;Ljava/lang/String;)V"))
            };
        assert!(! method_id . is_null (  ) ,
                "Can not find onStateChanged id");
        cb.methods.push(method_id);
        Box::new(cb)
    }
}
#[allow(non_snake_case, unused_variables, unused_mut)]
#[no_mangle]
pub extern "C" fn Java_net_korul_hbbft_Session_do_1subscribe(env: *mut JNIEnv,
                                                             _: jclass,
                                                             this: jlong,
                                                             a_0: jobject)
 -> () {
    let mut a_0: Box<OnEvent> = <Box<OnEvent>>::swig_from(a_0, env);
    let this: &mut Session =
        unsafe { jlong_to_pointer::<Session>(this).as_mut().unwrap() };
    let mut ret: () = Session::subscribe(this, a_0);
    ret
}
#[allow(non_snake_case, unused_variables, unused_mut)]
#[no_mangle]
pub extern "C" fn Java_net_korul_hbbft_Session_do_1after_1subscribe(env:
                                                                        *mut JNIEnv,
                                                                    _: jclass,
                                                                    this:
                                                                        jlong)
 -> () {
    let this: &mut Session =
        unsafe { jlong_to_pointer::<Session>(this).as_mut().unwrap() };
    let mut ret: () = Session::after_subscribe(this);
    ret
}
impl SwigInto<bool> for jboolean {
    fn swig_into(self, _: *mut JNIEnv) -> bool { self != 0 }
}
#[allow(non_snake_case, unused_variables, unused_mut)]
#[no_mangle]
pub extern "C" fn Java_net_korul_hbbft_Session_do_1change(env: *mut JNIEnv,
                                                          _: jclass,
                                                          this: jlong,
                                                          a_0: jint,
                                                          a_1: jboolean,
                                                          a_2: jstring,
                                                          a_3: jstring)
 -> () {
    let mut a_0: i32 = a_0.swig_into(env);
    let mut a_1: bool = a_1.swig_into(env);
    let mut a_2: JavaString = a_2.swig_into(env);
    let mut a_2: &str = a_2.swig_deref();
    let mut a_2: String = a_2.swig_into(env);
    let mut a_3: JavaString = a_3.swig_into(env);
    let mut a_3: &str = a_3.swig_deref();
    let mut a_3: String = a_3.swig_into(env);
    let this: &Session =
        unsafe { jlong_to_pointer::<Session>(this).as_mut().unwrap() };
    let mut ret: () = Session::change(this, a_0, a_1, a_2, a_3);
    ret
}
#[allow(unused_variables, unused_mut, non_snake_case)]
#[no_mangle]
pub extern "C" fn Java_net_korul_hbbft_Session_do_1delete(env: *mut JNIEnv,
                                                          _: jclass,
                                                          this: jlong) {
    let this: *mut Session =
        unsafe { jlong_to_pointer::<Session>(this).as_mut().unwrap() };
    let this: Box<Session> = unsafe { Box::from_raw(this) };
    drop(this);
}
