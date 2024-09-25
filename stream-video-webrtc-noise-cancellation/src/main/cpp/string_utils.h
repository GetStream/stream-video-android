//
// Created by Kanat Kiialbaev on 2024-09-25.
//
#include <string>
#include <locale>
#include <codecvt>

#ifndef STREAM_VIDEO_ANDROID_UTILS_H
#define STREAM_VIDEO_ANDROID_UTILS_H

namespace string_utils {

    std::wstring convertMBString2WString(const std::string &str);

    std::string convertMBString2String(const std::string &str);

    std::string convertWStringToString(const std::wstring& wstr);


} // utils

#endif //STREAM_VIDEO_ANDROID_UTILS_H
