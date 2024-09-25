//
// Created by Kanat Kiialbaev on 2024-09-25.
//

#include <chrono>
#include "time_utils.h"


namespace time_utils {
    // Returns the current time in milliseconds in 64 bits.
    int64_t TimeMillis() {
        auto now = std::chrono::system_clock::now();

        // Convert the time point to a duration since epoch
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch());

        // Return the duration count as an int64_t value
        return duration.count();
    }
};
