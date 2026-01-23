use jni::objects::{JByteArray, JClass, JString, ReleaseMode};
use jni::sys::{jbyteArray, jlong};
use jni::JNIEnv;
use serde::Serialize;
use serde_json;
use tokenizers::Tokenizer;

#[derive(Serialize)]
struct TokenizationResult {
    ids: Vec<u32>,
    attention_mask: Vec<u32>,
    token_type_ids: Vec<u32>
}

#[no_mangle]
pub extern "C" fn Java_com_ml_shubham0204_sentence_1embeddings_HFTokenizer_createTokenizer<'a>(
    mut env: JNIEnv<'a>,
    _: JClass<'a>,
    tokenizer_bytes: JByteArray<'a>,
) -> jlong {
    unsafe {
        let tokenizer_bytes_rs: Vec<u8> = env
            .get_array_elements(&tokenizer_bytes, ReleaseMode::CopyBack)
            .expect("Could not read tokenizer_bytes")
            .iter()
            .map(|x| *x as u8)
            .collect();
        Box::into_raw(Box::new(Tokenizer::from_bytes(&tokenizer_bytes_rs))) as jlong
    }
}

#[no_mangle]
pub extern "C" fn Java_com_ml_shubham0204_sentence_1embeddings_HFTokenizer_tokenize<'a>(
    mut env: JNIEnv<'a>,
    _: JClass<'a>,
    tokenizer_ptr: jlong,
    text: JString<'a>,
) -> JString<'a> {
    let tokenizer = unsafe { &mut *(tokenizer_ptr as *mut Tokenizer) };
    let text: String = env
        .get_string(&text)
        .expect("Could not convert text to Rust String")
        .into();
    let encoding = tokenizer.encode(text, true).expect("Could not encode text");
    let result = TokenizationResult {
        ids: encoding.get_ids().to_vec(),
        attention_mask: encoding.get_attention_mask().to_vec(),
        token_type_ids: encoding.get_type_ids().to_vec()
    };
    let result_json_str =
        serde_json::to_string(&result).expect("Could not convert tokenization result to JSON");
    env.new_string(result_json_str)
        .expect("Could not convert result_json_str to jstring")
}

#[no_mangle]
pub extern "C" fn Java_com_ml_shubham0204_sentence_1embeddings_HFTokenizer_deleteTokenizer(
    _: JNIEnv,
    _: JClass,
    tokenizer_ptr: jlong,
) {
    let _ptr = unsafe { Box::from_raw(tokenizer_ptr as *mut Tokenizer) };
}
