package com.thewizrd.shared_resources.controls;

import com.thewizrd.shared_resources.weatherdata.MoonPhase;

public class MoonPhaseViewModel {
    private DetailItemViewModel moonPhase;
    private MoonPhase.MoonPhaseType phaseType;

    public MoonPhaseViewModel(MoonPhase moonPhase) {
        this.moonPhase = new DetailItemViewModel(moonPhase.getPhase(), moonPhase.getDescription());
        this.phaseType = moonPhase.getPhase();
    }

    public DetailItemViewModel getMoonPhase() {
        return moonPhase;
    }

    public void setMoonPhase(DetailItemViewModel moonPhase) {
        this.moonPhase = moonPhase;
    }

    public MoonPhase.MoonPhaseType getPhaseType() {
        return phaseType;
    }

    public void setPhaseType(MoonPhase.MoonPhaseType phaseType) {
        this.phaseType = phaseType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MoonPhaseViewModel that = (MoonPhaseViewModel) o;

        if (moonPhase != null ? !moonPhase.equals(that.moonPhase) : that.moonPhase != null)
            return false;
        return phaseType == that.phaseType;
    }

    @Override
    public int hashCode() {
        int result = moonPhase != null ? moonPhase.hashCode() : 0;
        result = 31 * result + (phaseType != null ? phaseType.hashCode() : 0);
        return result;
    }
}
