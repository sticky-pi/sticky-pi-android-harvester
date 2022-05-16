package com.example.sticky_pi_data_harvester;

import java.util.Comparator;

public class FileComparators {
    private FileComparators() {
        //
    }

    // compare by device id
    public static Comparator<FileHandler> getDeviceIDComparator() {
        return new DeviceIDComparator();
    }

    // compare by number of images
    public static Comparator<FileHandler> getNofImagesComparator() {
        return new NofImagesComparator();
    }

    // compare by percent uploaded
    public static Comparator<FileHandler> getPercentUploadedComparator() {
        return new PercentUploadedComparator();
    }

    private static class DeviceIDComparator implements Comparator<FileHandler> {
        @Override
        public int compare(FileHandler f1, FileHandler f2) {
            return f1.get_device_id().compareTo(f2.get_device_id());
        }
    }

    private static class PercentUploadedComparator  implements Comparator<FileHandler> {
        @Override
        public int compare(FileHandler f1, FileHandler f2) {
            double p1 = (double) f1.get_n_jpg_images() / (f1.get_n_jpg_images() + f1.get_n_trace_images());
            double p2 = (double) f2.get_n_jpg_images() / (f2.get_n_jpg_images() + f2.get_n_trace_images());
            return Double.compare(p1, p2);
        }
    }

        private static class NofImagesComparator implements Comparator<FileHandler> {
            @Override
            public int compare(FileHandler f1, FileHandler f2) {
                return Integer.compare(f1.get_n_jpg_images(), f2.get_n_jpg_images());
            }
        }

}
