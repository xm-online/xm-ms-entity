package com.icthh.xm.ms.entity.lep;

import com.icthh.lep.api.LepResourceType;

/**
 * The {@link XmLepResourceType} enum.
 */
public enum XmLepResourceType implements LepResourceType {

    GROOVY {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return "text/groovy";
        }
    },

    JAVASCRIPT {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return "text/javascript";
        }
    },

    COMPOSITE {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return "application/composite";
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getName();
    }

}
