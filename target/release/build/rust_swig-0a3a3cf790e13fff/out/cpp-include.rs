mod swig_foreign_types_map { }
#[allow(unused_macros)]
macro_rules! swig_c_str(( $ lit : expr ) => {
                        concat ! ( $ lit , "\0" ) . as_ptr (  ) as * const ::
                        std :: os :: raw :: c_char } ;);
#[allow(dead_code)]
pub trait SwigForeignClass {
    fn c_class_name()
    -> *const ::std::os::raw::c_char;
    fn box_object(x: Self)
    -> *mut ::std::os::raw::c_void;
    fn unbox_object(p: *mut ::std::os::raw::c_void)
    -> Self;
}
#[allow(dead_code)]
trait SwigInto<T> {
    fn swig_into(self)
    -> T;
}
#[allow(dead_code)]
trait SwigFrom<T> {
    fn swig_from(T)
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
impl SwigInto<bool> for ::std::os::raw::c_char {
    fn swig_into(self) -> bool { self != 0 }
}
impl SwigFrom<bool> for ::std::os::raw::c_char {
    fn swig_from(x: bool) -> Self { if x { 1 } else { 0 } }
}
impl <'a> SwigInto<&'a ::std::ffi::CStr> for *const ::std::os::raw::c_char {
    fn swig_into(self) -> &'a ::std::ffi::CStr {
        assert!(! self . is_null (  ));
        unsafe { ::std::ffi::CStr::from_ptr(self) }
    }
}
impl <'a> SwigDeref for &'a ::std::ffi::CStr {
    type
    Target
    =
    str;
    fn swig_deref(&self) -> &Self::Target {
        self.to_str().expect("wrong utf-8")
    }
}
impl <'a> SwigInto<Option<&'a str>> for *const ::std::os::raw::c_char {
    fn swig_into(self) -> Option<&'a str> {
        if !self.is_null() {
            let n =
                {
                    let cstr = unsafe { ::std::ffi::CStr::from_ptr(self) };
                    cstr.to_bytes().len()
                };
            let bytes =
                unsafe { ::std::slice::from_raw_parts(self as *const u8, n) };
            Some(::std::str::from_utf8(bytes).expect("wrong utf-8"))
        } else { None }
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct RustStrView {
    data: *const ::std::os::raw::c_char,
    len: usize,
}
impl <'a> SwigFrom<&'a str> for RustStrView {
    fn swig_from(s: &'a str) -> RustStrView {
        RustStrView{data: s.as_ptr() as *const ::std::os::raw::c_char,
                    len: s.len(),}
    }
}
impl <T> SwigDeref for Arc<Mutex<T>> {
    type
    Target
    =
    Mutex<T>;
    fn swig_deref(&self) -> &Mutex<T> { self }
}
impl <'a, T> SwigFrom<&'a Mutex<T>> for MutexGuard<'a, T> {
    fn swig_from(m: &'a Mutex<T>) -> MutexGuard<'a, T> { m.lock().unwrap() }
}
impl <'a, T> SwigDeref for MutexGuard<'a, T> {
    type
    Target
    =
    T;
    fn swig_deref(&self) -> &T { self }
}
impl <'a, T> SwigDerefMut for MutexGuard<'a, T> {
    type
    Target
    =
    T;
    fn swig_deref_mut(&mut self) -> &mut T { self }
}
impl <T> SwigDeref for Rc<T> {
    type
    Target
    =
    T;
    fn swig_deref(&self) -> &T { self }
}
impl <'a, T> SwigDeref for &'a Rc<T> {
    type
    Target
    =
    T;
    fn swig_deref(&self) -> &T { self }
}
impl <'a, T> SwigFrom<&'a RefCell<T>> for Ref<'a, T> {
    fn swig_from(m: &'a RefCell<T>) -> Ref<'a, T> { m.borrow() }
}
impl <'a, T> SwigFrom<&'a RefCell<T>> for RefMut<'a, T> {
    fn swig_from(m: &'a RefCell<T>) -> RefMut<'a, T> { m.borrow_mut() }
}
impl <'a, T> SwigDeref for Ref<'a, T> {
    type
    Target
    =
    T;
    fn swig_deref(&self) -> &T { self }
}
impl <'a, T> SwigDerefMut for RefMut<'a, T> {
    type
    Target
    =
    T;
    fn swig_deref_mut(&mut self) -> &mut T { self }
}
impl <T: SwigForeignClass> SwigDeref for T {
    type
    Target
    =
    T;
    fn swig_deref(&self) -> &T { self }
}
impl <T: SwigForeignClass> SwigDerefMut for T {
    type
    Target
    =
    T;
    fn swig_deref_mut(&mut self) -> &mut T { self }
}
impl <T: SwigForeignClass> SwigFrom<T> for *mut ::std::os::raw::c_void {
    fn swig_from(x: T) -> Self { <T>::box_object(x) }
}
impl <'a, T: SwigForeignClass> SwigFrom<&'a T> for
 *const ::std::os::raw::c_void {
    fn swig_from(x: &'a T) -> Self {
        (x as *const T) as *const ::std::os::raw::c_void
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct CRustVecU8 {
    data: *const u8,
    len: usize,
    capacity: usize,
}
impl SwigFrom<Vec<u8>> for CRustVecU8 {
    fn swig_from(mut v: Vec<u8>) -> CRustVecU8 {
        let p = v.as_mut_ptr();
        let len = v.len();
        let cap = v.capacity();
        ::std::mem::forget(v);
        CRustVecU8{data: p, len: len, capacity: cap,}
    }
}
#[allow(private_no_mangle_fns)]
#[no_mangle]
pub extern "C" fn CRustVecU8_free(v: CRustVecU8) {
    let v =
        unsafe { Vec::from_raw_parts(v.data as *mut u8, v.len, v.capacity) };
    drop(v);
}
#[allow(dead_code)]
#[repr(C)]
pub struct CRustVecU32 {
    data: *const u32,
    len: usize,
    capacity: usize,
}
impl SwigFrom<Vec<u32>> for CRustVecU32 {
    fn swig_from(mut v: Vec<u32>) -> CRustVecU32 {
        let p = v.as_mut_ptr();
        let len = v.len();
        let cap = v.capacity();
        ::std::mem::forget(v);
        CRustVecU32{data: p, len: len, capacity: cap,}
    }
}
#[allow(private_no_mangle_fns)]
#[no_mangle]
pub extern "C" fn CRustVecU32_free(v: CRustVecU32) {
    let v =
        unsafe { Vec::from_raw_parts(v.data as *mut u32, v.len, v.capacity) };
    drop(v);
}
#[allow(dead_code)]
#[repr(C)]
pub struct CRustVecUsize {
    data: *const usize,
    len: usize,
    capacity: usize,
}
impl SwigFrom<Vec<usize>> for CRustVecUsize {
    fn swig_from(mut v: Vec<usize>) -> CRustVecUsize {
        let p = v.as_mut_ptr();
        let len = v.len();
        let cap = v.capacity();
        ::std::mem::forget(v);
        CRustVecUsize{data: p, len: len, capacity: cap,}
    }
}
#[allow(private_no_mangle_fns)]
#[no_mangle]
pub extern "C" fn CRustVecUsize_free(v: CRustVecUsize) {
    let v =
        unsafe {
            Vec::from_raw_parts(v.data as *mut usize, v.len, v.capacity)
        };
    drop(v);
}
#[allow(dead_code)]
#[repr(C)]
pub struct CRustVecF32 {
    data: *const f32,
    len: usize,
    capacity: usize,
}
impl SwigFrom<Vec<f32>> for CRustVecF32 {
    fn swig_from(mut v: Vec<f32>) -> CRustVecF32 {
        let p = v.as_mut_ptr();
        let len = v.len();
        let cap = v.capacity();
        ::std::mem::forget(v);
        CRustVecF32{data: p, len: len, capacity: cap,}
    }
}
#[allow(private_no_mangle_fns)]
#[no_mangle]
pub extern "C" fn CRustVecF32_free(v: CRustVecF32) {
    let v =
        unsafe { Vec::from_raw_parts(v.data as *mut f32, v.len, v.capacity) };
    drop(v);
}
#[allow(dead_code)]
#[repr(C)]
pub struct CRustVecF64 {
    data: *const f64,
    len: usize,
    capacity: usize,
}
impl SwigFrom<Vec<f64>> for CRustVecF64 {
    fn swig_from(mut v: Vec<f64>) -> CRustVecF64 {
        let p = v.as_mut_ptr();
        let len = v.len();
        let cap = v.capacity();
        ::std::mem::forget(v);
        CRustVecF64{data: p, len: len, capacity: cap,}
    }
}
#[allow(private_no_mangle_fns)]
#[no_mangle]
pub extern "C" fn CRustVecF64_free(v: CRustVecF64) {
    let v =
        unsafe { Vec::from_raw_parts(v.data as *mut f64, v.len, v.capacity) };
    drop(v);
}
#[allow(dead_code)]
#[repr(C)]
#[derive(Copy, Clone)]
pub struct CRustForeignVec {
    data: *const ::std::os::raw::c_void,
    len: usize,
    capacity: usize,
    step: usize,
}
#[allow(dead_code)]
impl CRustForeignVec {
    pub fn from_vec<T: SwigForeignClass>(mut v: Vec<T>) -> CRustForeignVec {
        let data = v.as_mut_ptr() as *const ::std::os::raw::c_void;
        let len = v.len();
        let capacity = v.capacity();
        ::std::mem::forget(v);
        CRustForeignVec{data,
                        len,
                        capacity,
                        step: ::std::mem::size_of::<T>(),}
    }
}
impl <T: SwigForeignClass> SwigFrom<Vec<T>> for CRustForeignVec {
    fn swig_from(v: Vec<T>) -> Self { CRustForeignVec::from_vec(v) }
}
#[allow(dead_code)]
fn drop_foreign_class_vec<T: SwigForeignClass>(data: *mut T, len: usize,
                                               cap: usize) {
    let v = unsafe { Vec::from_raw_parts(data, len, cap) };
    drop(v);
}
#[allow(dead_code)]
#[inline]
fn push_foreign_class_to_vec<T: SwigForeignClass>(vec: *mut CRustForeignVec,
                                                  elem:
                                                      *mut ::std::os::raw::c_void) {
    assert!(! vec . is_null (  ));
    let vec: &mut CRustForeignVec = unsafe { &mut *vec };
    assert_eq!(:: std :: mem :: size_of :: < T > (  ) , vec . step);
    let mut v =
        unsafe {
            Vec::from_raw_parts(vec.data as *mut T, vec.len, vec.capacity)
        };
    v.push(T::unbox_object(elem));
    vec.data = v.as_mut_ptr() as *const ::std::os::raw::c_void;
    vec.len = v.len();
    vec.capacity = v.capacity();
    ::std::mem::forget(v);
}
#[allow(dead_code)]
#[inline]
fn remove_foreign_class_from_vec<T: SwigForeignClass>(vec:
                                                          *mut CRustForeignVec,
                                                      index: usize)
 -> *mut ::std::os::raw::c_void {
    assert!(! vec . is_null (  ));
    let vec: &mut CRustForeignVec = unsafe { &mut *vec };
    assert_eq!(:: std :: mem :: size_of :: < T > (  ) , vec . step);
    let mut v =
        unsafe {
            Vec::from_raw_parts(vec.data as *mut T, vec.len, vec.capacity)
        };
    let elem: T = v.remove(index);
    vec.data = v.as_mut_ptr() as *const ::std::os::raw::c_void;
    vec.len = v.len();
    vec.capacity = v.capacity();
    ::std::mem::forget(v);
    T::box_object(elem)
}
impl <'a> SwigInto<&'a Path> for &'a str {
    fn swig_into(self) -> &'a Path { Path::new(self) }
}
#[allow(dead_code)]
#[repr(C)]
#[derive(Copy, Clone)]
pub struct CRustString {
    data: *const ::std::os::raw::c_char,
    len: usize,
    capacity: usize,
}
#[allow(private_no_mangle_fns)]
#[no_mangle]
pub extern "C" fn crust_string_free(x: CRustString) {
    let s =
        unsafe {
            String::from_raw_parts(x.data as *mut u8, x.len, x.capacity)
        };
    drop(s);
}
#[allow(dead_code)]
impl CRustString {
    pub fn from_string(s: String) -> CRustString {
        let data = s.as_ptr() as *const ::std::os::raw::c_char;
        let len = s.len();
        let capacity = s.capacity();
        ::std::mem::forget(s);
        CRustString{data, len, capacity,}
    }
}
impl SwigFrom<String> for CRustString {
    fn swig_from(s: String) -> CRustString { CRustString::from_string(s) }
}
impl <T: SwigForeignClass> SwigFrom<Option<T>> for *mut ::std::os::raw::c_void
 {
    fn swig_from(x: Option<T>) -> Self {
        match x {
            Some(x) => <T>::box_object(x),
            None => ::std::ptr::null_mut(),
        }
    }
}
impl <T: SwigForeignClass> SwigInto<Option<T>> for *mut ::std::os::raw::c_void
 {
    fn swig_into(self) -> Option<T> {
        if !self.is_null() { Some(T::unbox_object(self)) } else { None }
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct CResultObjectString {
    is_ok: u8,
    data: CResultObjectStringUnion,
}
#[allow(dead_code)]
#[repr(C)]
#[derive(Copy, Clone)]
pub union CResultObjectStringUnion {
    pub ok: *mut ::std::os::raw::c_void,
    pub err: CRustString,
}
impl SwigFrom<Result<(), String>> for CResultObjectString {
    fn swig_from(x: Result<(), String>) -> Self {
        match x {
            Ok(_) =>
            CResultObjectString{is_ok: 1,
                                data:
                                    CResultObjectStringUnion{ok:
                                                                 ::std::ptr::null_mut(),},},
            Err(err) =>
            CResultObjectString{is_ok: 0,
                                data:
                                    CResultObjectStringUnion{err:
                                                                 CRustString::from_string(err),},},
        }
    }
}
impl <T: SwigForeignClass> SwigFrom<Result<T, String>> for CResultObjectString
 {
    fn swig_from(x: Result<T, String>) -> Self {
        match x {
            Ok(v) =>
            CResultObjectString{is_ok: 1,
                                data:
                                    CResultObjectStringUnion{ok:
                                                                 <T>::box_object(v),},},
            Err(err) =>
            CResultObjectString{is_ok: 0,
                                data:
                                    CResultObjectStringUnion{err:
                                                                 CRustString::from_string(err),},},
        }
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct CRustSliceU8 {
    data: *const u8,
    len: usize,
}
impl <'a> SwigInto<CRustSliceU8> for &'a [u8] {
    fn swig_into(self) -> CRustSliceU8 {
        CRustSliceU8{data: self.as_ptr(), len: self.len(),}
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct CRustSliceU32 {
    data: *const u32,
    len: usize,
}
impl <'a> SwigInto<CRustSliceU32> for &'a [u32] {
    fn swig_into(self) -> CRustSliceU32 {
        CRustSliceU32{data: self.as_ptr(), len: self.len(),}
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct CRustSliceUsize {
    data: *const usize,
    len: usize,
}
impl <'a> SwigInto<CRustSliceUsize> for &'a [usize] {
    fn swig_into(self) -> CRustSliceUsize {
        CRustSliceUsize{data: self.as_ptr(), len: self.len(),}
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct CRustObjectSlice {
    data: *const ::std::os::raw::c_void,
    len: usize,
    step: usize,
}
impl <'a, T: SwigForeignClass> SwigInto<CRustObjectSlice> for &'a [T] {
    fn swig_into(self) -> CRustObjectSlice {
        CRustObjectSlice{data: self.as_ptr() as *const ::std::os::raw::c_void,
                         len: self.len(),
                         step: ::std::mem::size_of::<T>(),}
    }
}
impl <'a, T: SwigForeignClass> SwigFrom<CRustObjectSlice> for &'a mut [T] {
    fn swig_from(x: CRustObjectSlice) -> &'a mut [T] {
        unsafe { ::std::slice::from_raw_parts_mut(x.data as *mut T, x.len) }
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct CResultCRustForeignVecString {
    is_ok: u8,
    data: CResultCRustForeignVecStringUnion,
}
#[allow(dead_code)]
#[repr(C)]
#[derive(Copy, Clone)]
pub union CResultCRustForeignVecStringUnion {
    pub ok: CRustForeignVec,
    pub err: CRustString,
}
impl <T: SwigForeignClass> SwigFrom<Result<Vec<T>, String>> for
 CResultCRustForeignVecString {
    fn swig_from(x: Result<Vec<T>, String>) -> Self {
        match x {
            Ok(v) =>
            CResultCRustForeignVecString{is_ok: 1,
                                         data:
                                             CResultCRustForeignVecStringUnion{ok:
                                                                                   CRustForeignVec::from_vec(v),},},
            Err(err) =>
            CResultCRustForeignVecString{is_ok: 0,
                                         data:
                                             CResultCRustForeignVecStringUnion{err:
                                                                                   CRustString::from_string(err),},},
        }
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct CRustOptionF64 {
    val: f64,
    is_some: u8,
}
impl SwigFrom<Option<f64>> for CRustOptionF64 {
    fn swig_from(x: Option<f64>) -> Self {
        match x {
            Some(x) => CRustOptionF64{val: x, is_some: 1,},
            None => CRustOptionF64{val: 0., is_some: 0,},
        }
    }
}
impl SwigInto<Option<f64>> for CRustOptionF64 {
    fn swig_into(self) -> Option<f64> {
        if self.is_some != 0 { Some(self.val) } else { None }
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct CRustOptionU32 {
    val: u32,
    is_some: u8,
}
impl SwigFrom<Option<u32>> for CRustOptionU32 {
    fn swig_from(x: Option<u32>) -> Self {
        match x {
            Some(x) => CRustOptionU32{val: x, is_some: 1,},
            None => CRustOptionU32{val: 0, is_some: 0,},
        }
    }
}
impl SwigInto<Option<u32>> for CRustOptionU32 {
    fn swig_into(self) -> Option<u32> {
        if self.is_some != 0 { Some(self.val) } else { None }
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct CRustOptionU64 {
    val: u64,
    is_some: u8,
}
impl SwigFrom<Option<u64>> for CRustOptionU64 {
    fn swig_from(x: Option<u64>) -> Self {
        match x {
            Some(x) => CRustOptionU64{val: x, is_some: 1,},
            None => CRustOptionU64{val: 0, is_some: 0,},
        }
    }
}
impl SwigInto<Option<u64>> for CRustOptionU64 {
    fn swig_into(self) -> Option<u64> {
        if self.is_some != 0 { Some(self.val) } else { None }
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct CRustOptionI64 {
    val: i64,
    is_some: u8,
}
impl SwigFrom<Option<i64>> for CRustOptionI64 {
    fn swig_from(x: Option<i64>) -> Self {
        match x {
            Some(x) => CRustOptionI64{val: x, is_some: 1,},
            None => CRustOptionI64{val: 0, is_some: 0,},
        }
    }
}
impl SwigInto<Option<i64>> for CRustOptionI64 {
    fn swig_into(self) -> Option<i64> {
        if self.is_some != 0 { Some(self.val) } else { None }
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct CRustOptionUSize {
    val: usize,
    is_some: u8,
}
impl SwigFrom<Option<usize>> for CRustOptionUSize {
    fn swig_from(x: Option<usize>) -> Self {
        match x {
            Some(x) => CRustOptionUSize{val: x, is_some: 1,},
            None => CRustOptionUSize{val: 0, is_some: 0,},
        }
    }
}
impl SwigInto<Option<usize>> for CRustOptionUSize {
    fn swig_into(self) -> Option<usize> {
        if self.is_some != 0 { Some(self.val) } else { None }
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct CResultObjectObject {
    is_ok: u8,
    data: CResultObjectObjectUnion,
}
#[allow(dead_code)]
#[repr(C)]
#[derive(Copy, Clone)]
pub union CResultObjectObjectUnion {
    pub ok: *mut ::std::os::raw::c_void,
    pub err: *mut ::std::os::raw::c_void,
}
impl <ErrT: SwigForeignClass> SwigFrom<Result<(), ErrT>> for
 CResultObjectObject {
    fn swig_from(x: Result<(), ErrT>) -> Self {
        match x {
            Ok(_) =>
            CResultObjectObject{is_ok: 1,
                                data:
                                    CResultObjectObjectUnion{ok:
                                                                 ::std::ptr::null_mut(),},},
            Err(err) =>
            CResultObjectObject{is_ok: 0,
                                data:
                                    CResultObjectObjectUnion{err:
                                                                 <ErrT>::box_object(err),},},
        }
    }
}
impl <T: SwigForeignClass, ErrT: SwigForeignClass> SwigFrom<Result<T, ErrT>>
 for CResultObjectObject {
    fn swig_from(x: Result<T, ErrT>) -> Self {
        match x {
            Ok(v) =>
            CResultObjectObject{is_ok: 1,
                                data:
                                    CResultObjectObjectUnion{ok:
                                                                 <T>::box_object(v),},},
            Err(err) =>
            CResultObjectObject{is_ok: 0,
                                data:
                                    CResultObjectObjectUnion{err:
                                                                 <ErrT>::box_object(err),},},
        }
    }
}
#[allow(dead_code)]
#[repr(C)]
pub struct CResultVecObjectObject {
    is_ok: u8,
    data: CResultVecObjectObjectUnion,
}
#[allow(dead_code)]
#[repr(C)]
#[derive(Copy, Clone)]
pub union CResultVecObjectObjectUnion {
    pub ok: CRustForeignVec,
    pub err: *mut ::std::os::raw::c_void,
}
impl <T: SwigForeignClass, ErrT: SwigForeignClass>
 SwigFrom<Result<Vec<T>, ErrT>> for CResultVecObjectObject {
    fn swig_from(x: Result<Vec<T>, ErrT>) -> Self {
        match x {
            Ok(v) =>
            CResultVecObjectObject{is_ok: 1,
                                   data:
                                       CResultVecObjectObjectUnion{ok:
                                                                       CRustForeignVec::from_vec(v),},},
            Err(err) =>
            CResultVecObjectObject{is_ok: 0,
                                   data:
                                       CResultVecObjectObjectUnion{err:
                                                                       <ErrT>::box_object(err),},},
        }
    }
}
impl <'a> SwigInto<String> for &'a str {
    fn swig_into(self) -> String { self.into() }
}