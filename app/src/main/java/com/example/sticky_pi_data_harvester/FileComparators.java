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
    public static Comparator<FileHandler> getImagesComparator() {
        return new NofImagesComparator();
    }

    // compare by percent uploaded
    public static Comparator<FileHandler> getPercentUploadedComparator() {
        return new PercentUploadedComparator();
    }

    public static Comparator<FileHandler> getLastSeenComparator() {
        return new LastSeenComparator();
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

    private static class LastSeenComparator implements Comparator<FileHandler> {
        @Override
        public int compare(FileHandler f1, FileHandler f2) {
            long f1_time = f1.get_last_seen();
            long f2_time = f2.get_last_seen();
            if (f1_time < f2_time) {
                return -1;
            } else if(f1_time > f2_time) {
                return 1;
            }
            return 0;
        }
    }
}
