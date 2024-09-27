//
// Created by Kanat Kiialbaev on 2024-09-25.
//

#include "string_utils.h"

namespace string_utils {

    // Convert multibyte std::string to std::wstring
    std::wstring convertMBStringToWString(const std::string &str) {
        std::wstring w(str.begin(), str.end());
        return w;
    }

    // Convert multibyte std::string to std::string
    std::string convertMBStringToString(const std::string &str) {
        std::string result(str.begin(), str.end());
        return result;
    }

    // Convert std::wstring to UTF-8 std::string for logging
    std::string convertWStringToString(const std::wstring& wstr) {
        std::wstring_convert<std::codecvt_utf8_utf16<wchar_t>> converter;
        return converter.to_bytes(wstr);
    }

    // Convert std::string to std::wstring (UTF-8 to wide characters)
    std::wstring convertStringToWString(const std::string& str) {
        std::wstring_convert<std::codecvt_utf8_utf16<wchar_t>> converter;
        return converter.from_bytes(str);
    }

} // utils