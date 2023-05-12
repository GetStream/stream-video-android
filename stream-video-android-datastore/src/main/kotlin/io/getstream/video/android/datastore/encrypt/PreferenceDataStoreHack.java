package io.getstream.video.android.datastore.encrypt;

import androidx.datastore.core.DataStore;
import androidx.datastore.core.Serializer;
import androidx.datastore.preferences.core.PreferenceDataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesSerializer;

import org.jetbrains.annotations.NotNull;

// We want to wrap PreferencesSerializer to our EncryptedSerializer, but can't access it from Kotlin
// because it has internal visibility modifier.
// We still can access it from Java, so we do this dirty hack to bypass internal visibility.
// Package-private visibility used to make this hack visible only to our own Kotlin code in the same package.
@SuppressWarnings({"KotlinInternalInJava", "unused"})
class PreferenceDataStoreHack {

    static Serializer<Preferences> serializer = PreferencesSerializer.INSTANCE;
    static String fileExtension = PreferencesSerializer.INSTANCE.getFileExtension();

    private PreferenceDataStoreHack() {
        // Shouldn't be instantiated
    }

    @NotNull
    static DataStore<Preferences> wrap(DataStore<Preferences> delegate) {
        return new PreferenceDataStore(delegate);
    }
}