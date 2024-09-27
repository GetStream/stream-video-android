//
// Created by Kanat Kiialbaev on 2024-09-25.
//
#include <string>
#include <locale>
#include <codecvt>

#ifndef STREAM_VIDEO_ANDROID_UTILS_H
#define STREAM_VIDEO_ANDROID_UTILS_H

namespace string_utils {

    std::wstring convertMBStringToWString(const std::string &str);

    std::string convertMBStringToString(const std::string &str);

    std::string convertWStringToString(const std::wstring& wstr);

    std::wstring convertStringToWString(const std::string& str);


} // utils

#endif //STREAM_VIDEO_ANDROID_UTILS_H
