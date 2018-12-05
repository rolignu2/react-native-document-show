using ReactNative.Bridge;
using System;
using System.Collections.Generic;
using Windows.ApplicationModel.Core;
using Windows.UI.Core;

namespace React.Native.Document.Show.RNReactNativeDocumentShow
{
    /// <summary>
    /// A module that allows JS to share data.
    /// </summary>
    class RNReactNativeDocumentShowModule : NativeModuleBase
    {
        /// <summary>
        /// Instantiates the <see cref="RNReactNativeDocumentShowModule"/>.
        /// </summary>
        internal RNReactNativeDocumentShowModule()
        {

        }

        /// <summary>
        /// The name of the native module.
        /// </summary>
        public override string Name
        {
            get
            {
                return "RNReactNativeDocumentShow";
            }
        }
    }
}
