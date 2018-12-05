
'use strict';

import {Platform }          from 'react-native';


export class DocumentUntils  {

    static Files = {
        All         : Platform.OS == 'android' ? "*/*"              : "public.content",
        Text        : Platform.OS == 'android' ? "text/plain"       : "public.plain-text",
        Images      : Platform.OS == 'android' ? "image/*"          : "public.image",
        Audio       : Platform.OS == 'android' ? "audio/*"          : "public.audio",
        Pdf         : Platform.OS == 'android' ? "application/pdf"  : "public.image",
    }

}
