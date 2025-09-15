import { s } from 'msw/lib/core/HttpResponse-DWu36LsY';

import { useLocationsContext } from '@entities/location/contexts/useLocationsContext';
import { StartingPlace } from '@entities/location/types/Location';

import StartingSpotName from '@shared/components/startingSpotName/StartingSpotName';
import { flex, typography } from '@shared/styles/default.styled';

import * as startSpotWrapper from './startingSpotWrapper.styled';

interface StaringSpotWrapperProps {
  startingPlaces: StartingPlace[];
}

function StartingSpotWrapper({ startingPlaces }: StaringSpotWrapperProps) {
  return (
    <div
      css={[startSpotWrapper.base(), flex({ direction: 'column', gap: 10 })]}
    >
      <div css={[flex({ align: 'center', gap: 10 })]}>
        <span css={[typography.sh1, startSpotWrapper.title()]}>출발지</span>
        <div css={[flex({ wrap: 'wrap', gap: 5 })]}>
          {startingPlaces.map((place, index) => {
            const isLast = startingPlaces.length - 1 === index;
            return (
              <StartingSpotName
                key={place.index}
                place={place}
                isLast={isLast}
              />
            );
          })}
        </div>
      </div>
      <div css={[flex({ align: 'center', gap: 20 })]}>
        <span css={[typography.sh1, startSpotWrapper.title()]}>조건</span>
        <span css={[typography.b2, startSpotWrapper.content()]}>{}장소</span>
      </div>
    </div>
  );
}

export default StartingSpotWrapper;
